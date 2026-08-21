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
package com.alipay.sofa.rpc.profile.jfr;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.module.JfrProfileModule;
import com.alipay.sofa.rpc.profile.Profiles;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcServerEvent;
import com.alipay.sofa.rpc.profile.jfr.report.JfrProfileReporter;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.alipay.sofa.rpc.triple.TripleHessianInterface;
import com.alipay.sofa.rpc.triple.TripleHessianInterfaceImpl;
import jdk.jfr.Recording;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 验证真实Bolt和Triple调用可以生成并关联JFR客户端、服务端事件。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileIntegrationTest extends ActivelyDestroyTest {

    private static final AtomicInteger PORT = new AtomicInteger(52080);

    private JfrProfileModule           module;

    private Recording                  recording;

    @Before
    public void before() throws Exception {
        recording = new Recording();
        recording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        recording.enable(SofaRpcServerEvent.class).withoutThreshold().withoutStackTrace();
        recording.start();

        module = new JfrProfileModule();
        module.install();
        Assert.assertTrue(Profiles.isEnabled());
    }

    @After
    public void after() {
        if (module != null) {
            module.uninstall();
        }
        if (recording != null) {
            recording.close();
        }
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void recordAndReportBoltAndTripleInvocations() throws Exception {
        invokeBolt(PORT.getAndIncrement());
        invokeTriple(PORT.getAndIncrement());

        Path recordingFile = Files.createTempFile("sofa-rpc-profile-integration", ".jfr");
        try {
            recording.stop();
            recording.dump(recordingFile);
            String report = JfrProfileReporter.createReport(Collections.singletonList(recordingFile), 20);

            Assert.assertTrue(report, report.contains("RPC events scanned: 4"));
            Assert.assertTrue(report, report.contains("invocation groups: 2"));
            Assert.assertTrue(report, report.contains("correlated client/server: 2"));
            Assert.assertTrue(report, report.contains(HelloService.class.getName()));
            Assert.assertTrue(report, report.contains("#sayHello"));
            Assert.assertTrue(report, report.contains(TripleHessianInterface.class.getName()));
            Assert.assertTrue(report, report.contains("#call1"));
            Assert.assertTrue(report, report.contains("[bolt/sync]"));
            Assert.assertTrue(report, report.contains("[tri/sync]"));
            Assert.assertTrue(report, report.contains("request serialization"));
            Assert.assertTrue(report, report.contains("request deserialization"));
            Assert.assertTrue(report, report.contains("response serialization"));
            Assert.assertTrue(report, report.contains("response deserialization"));
            Assert.assertTrue(report, report.contains("business invocation"));
            Assert.assertFalse(report, report.contains("client: not recorded"));
            Assert.assertFalse(report, report.contains("server: not recorded"));
        } finally {
            Files.deleteIfExists(recordingFile);
        }
    }

    private static void invokeBolt(int port) {
        ApplicationConfig providerApplication = new ApplicationConfig().setAppName("profile-bolt-provider");
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setPort(port)
            .setStopTimeout(0);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(providerApplication)
            .setRef(new HelloServiceImpl(20))
            .setServer(serverConfig)
            .setRegister(false);
        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(new ApplicationConfig().setAppName("profile-bolt-consumer"))
            .setDirectUrl("bolt://127.0.0.1:" + port + "?appName=profile-bolt-provider")
            .setTimeout(3000)
            .setRegister(false);

        providerConfig.export();
        try {
            HelloService service = consumerConfig.refer();
            try {
                Assert.assertEquals("hello profile from server! age: 1128", service.sayHello("profile", 1128));
            } finally {
                consumerConfig.unRefer();
            }
        } finally {
            providerConfig.unExport();
            serverConfig.destroy();
        }
    }

    private static void invokeTriple(int port) {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port)
            .setStopTimeout(0);
        ProviderConfig<TripleHessianInterface> providerConfig = new ProviderConfig<TripleHessianInterface>()
            .setApplication(new ApplicationConfig().setAppName("profile-triple-provider"))
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setRef(new TripleHessianInterfaceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        ConsumerConfig<TripleHessianInterface> consumerConfig = new ConsumerConfig<TripleHessianInterface>()
            .setApplication(new ApplicationConfig().setAppName("profile-triple-consumer"))
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port + "?appName=profile-triple-provider")
            .setTimeout(3000)
            .setRegister(false);

        providerConfig.export();
        try {
            TripleHessianInterface service = consumerConfig.refer();
            try {
                Assert.assertEquals("call1", service.call1());
            } finally {
                consumerConfig.unRefer();
            }
        } finally {
            providerConfig.unExport();
            serverConfig.destroy();
        }
    }
}
