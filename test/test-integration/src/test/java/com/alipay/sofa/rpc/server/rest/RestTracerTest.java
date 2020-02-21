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

import com.alibaba.fastjson.JSONObject;
import com.alipay.common.tracer.core.SofaTracer;
import com.alipay.common.tracer.core.reporter.digest.DiskReporterImpl;
import com.alipay.common.tracer.core.reporter.facade.Reporter;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.tracer.util.TracerChecker;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.RpcSofaTracer;
import com.alipay.sofa.rpc.tracer.sofatracer.factory.MemoryReporterImpl;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RestTracerTest extends ActivelyDestroyTest {

    private MemoryReporterImpl memoryReporter;

    private DiskReporterImpl   diskReporter;

    private Field              tracerField         = null;

    private Field              clientReporterField = null;

    private Field              serverReporterField = null;

    private SofaTracer         tracer              = null;

    @Before
    public void before() {

        System.setProperty("reporter_type", "MEMORY");
    }

    @Test
    public void testRestTracer() throws InterruptedException, IOException {

        Reporter clientReporter = reflectToTracer();
        memoryReporter = (MemoryReporterImpl) clientReporter;

        ServerConfig restServer = new ServerConfig()
            .setPort(8583)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST);

        List<ServerConfig> servers = new ArrayList<ServerConfig>(2);
        servers.add(restServer);

        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setRegister(false)
            .setServer(servers);
        providerConfig.export();

        //rest服务
        ConsumerConfig<RestService> consumerConfigRest = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
            .setDirectUrl("rest://127.0.0.1:8583")
            .setTimeout(1000)
            .setApplication(new ApplicationConfig().setAppName("TestClientRest"));
        final RestService restServiceRest = consumerConfigRest.refer();

        restServiceRest.get("test");

        final int times = 10;
        final CountDownLatch latch = new CountDownLatch(times);
        final AtomicInteger success = new AtomicInteger(0);
        for (int i = 0; i < times; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < times; j++) {
                            final String ok_rest = restServiceRest.get("ok_rest");
                            Assert.assertEquals("serverok_rest", ok_rest);
                            success.incrementAndGet();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        latch.await();
        Assert.assertEquals(times * times, success.get());

        TimeUnit.SECONDS.sleep(5);

        List<String> clientDigestContents = memoryReporter.getClientDigestHolder();

        List<String> serverDigestContents = memoryReporter.getServerDigestHolder();

        List<JSONObject> clientDigest = TracerChecker.convertContents2Json(clientDigestContents);
        List<String> clientTraceIds = readTraceId(clientDigest);

        List<JSONObject> serverDigest = TracerChecker.convertContents2Json(serverDigestContents);

        List<String> serverTraceIds = readTraceId(serverDigest);

        Assert.assertTrue(CommonUtils.isNotEmpty(clientTraceIds));
        Assert.assertTrue(CommonUtils.isNotEmpty(serverTraceIds));

        HashSet<String> hashSet = new HashSet<String>(200);
        for (String clientTraceId : clientTraceIds) {
            //will not duplicate
            Assert.assertTrue(!hashSet.contains(clientTraceId));
            hashSet.add(clientTraceId);
            Assert.assertTrue(serverTraceIds.contains(clientTraceId));
        }

        //validate one rpc server and rpc client field

        boolean result = TracerChecker.validateTracerDigest(clientDigest.get(0), "client",
            RpcConstants.PROTOCOL_TYPE_REST);

        Assert.assertTrue(result);
        result = TracerChecker.validateTracerDigest(serverDigest.get(0), "server", RpcConstants.PROTOCOL_TYPE_REST);
        Assert.assertTrue(result);

    }

    /**
     * reflect to tracer
     *
     * @return
     */
    protected Reporter reflectToTracer() {
        try {
            Tracer rpcSofaTracer = Tracers.getTracer();
            tracerField = RpcSofaTracer.class.getDeclaredField("sofaTracer");
            tracerField.setAccessible(true);

            //OpenTracing tracer 标准实现
            tracer = (SofaTracer) tracerField.get(rpcSofaTracer);
            Reporter tempReport = tracer.getClientReporter();
            clientReporterField = SofaTracer.class.getDeclaredField("clientReporter");
            clientReporterField.setAccessible(true);
            serverReporterField = SofaTracer.class.getDeclaredField("serverReporter");
            serverReporterField.setAccessible(true);
            if (tempReport instanceof DiskReporterImpl) {
                diskReporter = (DiskReporterImpl) tempReport;
                assertNotNull(diskReporter);
                memoryReporter = new MemoryReporterImpl(null, null, null, null, diskReporter.getStatReporter());
                clientReporterField.set(tracer, memoryReporter);
                serverReporterField.set(tracer, memoryReporter);

            } else {
                memoryReporter = (MemoryReporterImpl) tempReport;
            }
            //否则说明已经是 memory 了.主要是本地

        } catch (Exception e) {
            e.printStackTrace();
        }

        return memoryReporter;
    }

    //readTracerDigest TraceId and spanId
    public List<String> readTraceId(List<JSONObject> jsonObjects) throws IOException {

        List<String> result = TracerChecker.extractFields(jsonObjects, "tracerId");

        return result;
    }

    @After
    public void after() {

        System.setProperty("reporter_type", "DISK");

        if (memoryReporter != null) {
            memoryReporter.clearAll();
        }

        try {
            clientReporterField.set(tracer, diskReporter);
            serverReporterField.set(tracer, diskReporter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}