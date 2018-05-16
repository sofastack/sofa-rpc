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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.common.tracer.core.appender.TracerLogRootDeamon;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.tracer.sofatracer.RpcSofaTracer;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.log.type.RpcTracerLogEnum;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RestTracerTest extends ActivelyDestroyTest {

    private static String logDirectory = TracerLogRootDeamon.LOG_FILE_DIR;

    @Before
    public void before() throws Exception {
        File traceLogDirectory = new File(logDirectory);
        if (!traceLogDirectory.exists()) {
            return;
        }
        FileUtils.cleanDirectory(traceLogDirectory);
        reflectSetNewTracer();
    }

    @Test
    public void testRestTracer() throws InterruptedException, IOException {

        ServerConfig restServer = new ServerConfig()
            .setPort(8583)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST);

        ServerConfig boltServer = new ServerConfig()
            .setPort(8993)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        List<ServerConfig> servers = new ArrayList<ServerConfig>(2);
        servers.add(restServer);
        servers.add(boltServer);

        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setRegister(false)
            .setServer(servers);
        providerConfig.export();

        Thread.sleep(3000);

        //rest服务
        ConsumerConfig<RestService> consumerConfigRest = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
            .setDirectUrl("rest://127.0.0.1:8583")
            .setTimeout(30000)
            .setApplication(new ApplicationConfig().setAppName("TestClientRest"));
        final RestService restServiceRest = consumerConfigRest.refer();

        //bolt服务
        ConsumerConfig<RestService> consumerConfigBolt = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setDirectUrl("bolt://127.0.0.1:8993")
            .setTimeout(30000)
            .setApplication(new ApplicationConfig().setAppName("TestClientBolt"));
        final RestService restServiceBolt = consumerConfigBolt.refer();
        final int times = 10;
        final CountDownLatch latch = new CountDownLatch(times * times);
        final int[] success = { 0 };
        for (int i = 0; i < times; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < times; j++) {
                            Assert.assertEquals("serverok_rest", restServiceRest.get("ok_rest"));
                            Assert.assertEquals("serverok_bolt", restServiceBolt.get("ok_bolt"));
                            success[0]++;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();

        }
        latch.await(10000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(success[0], times * times);

        //assret
        List<String> clientTraceIds = readTraceId(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName()));

        List<String> serverTraceIds = readTraceId(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_SERVER_DIGEST.getDefaultLogName()));

        HashSet<String> hashSet = new HashSet<String>(200);
        for (String clientTraceId : clientTraceIds) {

            Assert.assertEquals(true, hashSet.add(clientTraceId));
            Assert.assertEquals(true, serverTraceIds.contains(clientTraceId));

        }

    }

    public List<String> readTraceId(File file) throws IOException {

        List<String> traceIds = new ArrayList<String>();
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new FileReader(file);
            bufferedReader = new BufferedReader(reader);
            String lineText = null;
            while ((lineText = bufferedReader.readLine()) != null) {

                traceIds.add(lineText.split(",")[2]);
            }

            return traceIds;
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    protected void reflectSetNewTracer() throws Exception {
        Tracer newTracerInstance = new RpcSofaTracer();
        Field tracerField = Tracers.class.getDeclaredField("tracer");
        tracerField.setAccessible(true);
        tracerField.set(null, newTracerInstance);
    }

    @After
    public void after() {
        RpcRuntimeContext.destroy();
        File traceLogDirectory = new File(logDirectory);
        if (!traceLogDirectory.exists()) {
            return;
        }
        FileUtils.cleanDirectory(traceLogDirectory);
    }

}