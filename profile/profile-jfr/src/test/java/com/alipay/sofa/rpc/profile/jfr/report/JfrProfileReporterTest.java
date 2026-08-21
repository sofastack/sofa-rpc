/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.profile.jfr.report;

import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcServerEvent;
import jdk.jfr.Recording;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileReporterTest {

    @Test
    public void correlateAndSortRpcEvents() throws Exception {
        Recording clientRecording = new Recording();
        clientRecording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        Recording serverRecording = new Recording();
        serverRecording.enable(SofaRpcServerEvent.class).withoutThreshold().withoutStackTrace();
        Path clientFile = Files.createTempFile("sofa-rpc-profile-client", ".jfr");
        Path serverFile = Files.createTempFile("sofa-rpc-profile-server", ".jfr");
        try {
            clientRecording.start();
            SofaRpcClientEvent slowClient = clientEvent("slow-id", "slow");
            slowClient.begin();
            Thread.sleep(12L);
            slowClient.routerTime = 7_000_000L;
            slowClient.end();
            slowClient.commit();

            SofaRpcClientEvent fastClient = clientEvent("fast-id", "fast");
            fastClient.commit();

            clientRecording.stop();
            clientRecording.dump(clientFile);

            serverRecording.start();
            SofaRpcServerEvent slowServer = new SofaRpcServerEvent();
            slowServer.profileId = "slow-id";
            slowServer.service = "com.example.Service";
            slowServer.method = "slow";
            slowServer.result = "SUCCESS";
            slowServer.businessTime = 5_000_000L;
            slowServer.commit();
            SofaRpcServerEvent retryServer = new SofaRpcServerEvent();
            retryServer.profileId = "slow-id";
            retryServer.service = "com.example.Service";
            retryServer.method = "slow";
            retryServer.result = "EXCEPTION";
            retryServer.businessTime = 1_000_000L;
            retryServer.commit();
            serverRecording.stop();
            serverRecording.dump(serverFile);

            String report = JfrProfileReporter.createReport(Arrays.asList(clientFile, serverFile), 20);
            Assert.assertTrue(report.contains("JFR files: 2"));
            Assert.assertTrue(report.contains("invocation groups: 2"));
            Assert.assertTrue(report.contains("correlated client/server: 1"));
            Assert.assertTrue(report.contains("service=com.example.Service#slow"));
            Assert.assertTrue(report.contains("client: "));
            Assert.assertTrue(report.contains("server[1]: "));
            Assert.assertTrue(report.contains("server[2]: "));
            Assert.assertTrue(report.contains("largest recorded phase: client.router"));
            Assert.assertTrue(report.indexOf("profileId=slow-id") < report.indexOf("profileId=fast-id"));
        } finally {
            clientRecording.close();
            serverRecording.close();
            Files.deleteIfExists(clientFile);
            Files.deleteIfExists(serverFile);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireInputFile() throws Exception {
        JfrProfileReporter.createReport(Collections.<Path> emptyList(), 20);
    }

    @Test
    public void retainOnlyBoundedTopInvocationGroups() throws Exception {
        Recording recording = new Recording();
        recording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        Path file = Files.createTempFile("sofa-rpc-profile-bounded-top", ".jfr");
        try {
            recording.start();
            for (int i = 0; i < 2_000; i++) {
                clientEvent("fast-" + i, "fast" + i).commit();
            }

            SofaRpcClientEvent slower = clientEvent("slower-id", "slower");
            slower.begin();
            Thread.sleep(8L);
            slower.end();
            slower.commit();

            SofaRpcClientEvent slowest = clientEvent("slowest-id", "slowest");
            slowest.begin();
            Thread.sleep(12L);
            slowest.end();
            slowest.commit();

            recording.stop();
            recording.dump(file);

            String report = JfrProfileReporter.createReport(Collections.singletonList(file), 2);
            Assert.assertTrue(report, report.contains("RPC events scanned: 2002"));
            Assert.assertTrue(report, report.contains("invocation groups: 2 selected"));
            Assert.assertTrue(report, report.contains("profileId=slowest-id"));
            Assert.assertTrue(report, report.contains("profileId=slower-id"));
            Assert.assertFalse(report, report.contains("profileId=fast-"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    private static SofaRpcClientEvent clientEvent(String profileId, String method) {
        SofaRpcClientEvent event = new SofaRpcClientEvent();
        event.profileId = profileId;
        event.service = "com.example.Service";
        event.method = method;
        event.sourceApp = "consumer";
        event.targetApp = "provider";
        event.protocol = "bolt";
        event.invokeType = "sync";
        event.result = "SUCCESS";
        return event;
    }
}
