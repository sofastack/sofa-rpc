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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ClientStartInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.profile.ProfileConstants;
import com.alipay.sofa.rpc.profile.Profiles;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import com.alipay.sofa.rpc.profile.jfr.report.JfrProfileReporter;
import jdk.jfr.Recording;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileModuleTest {

    private String           oldProfile;

    private JfrProfileModule module;

    @Before
    public void before() {
        oldProfile = RpcConfigs.getStringValue(RpcOptions.DEFAULT_PROFILE);
        module = new JfrProfileModule();
    }

    @After
    public void after() {
        module.uninstall();
        RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, oldProfile);
        Profiles.uninstall();
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void loadOnlyForJfrProfile() {
        RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, "jfr");
        Assert.assertTrue(module.needLoad());

        RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, "");
        Assert.assertFalse(module.needLoad());

        RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, "otherProfile");
        Assert.assertFalse(module.needLoad());
    }

    @Test
    public void discoverFromSofaRpcSpi() {
        ExtensionClass<Module> extension = ExtensionLoaderFactory.getExtensionLoader(Module.class)
            .getExtensionClass("jfrProfile");
        Assert.assertNotNull(extension);
        Assert.assertTrue(extension.getExtInstance() instanceof JfrProfileModule);
    }

    @Test
    public void installAndUninstallEventSubscriber() throws Exception {
        Recording recording = new Recording();
        recording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        try {
            // ClientStartInvokeEvent会初始化RpcRuntimeContext。先在关闭状态下完成初始化，
            // 避免ModuleFactory与本测试显式安装的模块各注册一个订阅者。
            RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, "");
            SofaRequest request = new SofaRequest();
            request.setTargetServiceUniqueName("com.example.ModuleTestService:1.0");
            request.setMethodName("test");
            ClientStartInvokeEvent startEvent = new ClientStartInvokeEvent(request);

            recording.start();
            RpcConfigs.putValue(RpcOptions.DEFAULT_PROFILE, "jfr");
            module.install();
            module.install();

            Assert.assertTrue(Profiles.isEnabled());
            Assert.assertTrue(EventBus.isEnable(ClientStartInvokeEvent.class));

            EventBus.post(startEvent);
            Assert.assertNotNull(request.getRequestProp(ProfileConstants.PROFILE_ID_KEY));
            EventBus.post(new ClientEndInvokeEvent(request, new SofaResponse(), null));

            module.uninstall();
            Assert.assertFalse(Profiles.isEnabled());
            Assert.assertFalse(EventBus.isEnable(ClientStartInvokeEvent.class));
        } finally {
            recording.close();
        }
    }

    @Test
    public void autoDiscoverModuleInFreshJvm() throws Exception {
        Path recordingFile = Files.createTempFile("sofa-rpc-profile-auto-discovery", ".jfr");
        Path logFile = Files.createTempFile("sofa-rpc-profile-auto-discovery", ".log");
        try {
            String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("surefire.test.class.path");
            if (classpath == null || classpath.isEmpty()) {
                classpath = System.getProperty("java.class.path");
            }
            ProcessBuilder builder = new ProcessBuilder(java,
                "-Ddefault.profile=jfr",
                "-Dmodule.load.list=*",
                "-Djvm.shutdown.hook=false",
                "-cp", classpath,
                JfrProfileAutoDiscoveryMain.class.getName(), recordingFile.toString());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());
            Process process = builder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            String output = new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
            Assert.assertTrue("Auto-discovery JVM timed out:\n" + output, finished);
            Assert.assertEquals("Auto-discovery JVM failed:\n" + output, 0, process.exitValue());

            String report = JfrProfileReporter.createReport(Collections.singletonList(recordingFile), 1);
            Assert.assertTrue(report, report.contains("com.example.AutoDiscoveryService:1.0#hello"));
            Assert.assertTrue(report, report.contains("invocation groups: 1 selected"));
        } finally {
            Files.deleteIfExists(recordingFile);
            Files.deleteIfExists(logFile);
        }
    }
}
