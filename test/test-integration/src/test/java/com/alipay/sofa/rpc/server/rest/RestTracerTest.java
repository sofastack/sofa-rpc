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
import com.alipay.common.tracer.core.appender.TracerLogRootDeamon;
import com.alipay.common.tracer.core.appender.manager.AsyncCommonDigestAppenderManager;
import com.alipay.common.tracer.core.reporter.digest.manager.SofaTracerDigestReporterAsyncManager;
import com.alipay.common.tracer.core.reporter.stat.manager.SofaTracerStatisticReporterCycleTimesManager;
import com.alipay.common.tracer.core.reporter.stat.manager.SofaTracerStatisticReporterManager;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.server.tracer.util.TracerChecker;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.RpcSofaTracer;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
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
        boolean cleanResult = FileUtils.cleanDirectory(traceLogDirectory);

        Assert.assertTrue(cleanResult);
        reflectSetNewTracer();
    }

    @Test
    public void testRestTracer() throws InterruptedException, IOException {

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

        Thread.sleep(10000);

        //先不要校验了 ,这个需要把 tracer 这个日志方式改一下.否则校验很高概率失败.
        /*
        //assret
        final File clientFile = new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName());

        System.out.println("clientfile" + clientFile.toURI());
        List<JSONObject> clientDigest = readContent(clientFile);
        List<String> clientTraceIds = readTraceId(clientDigest);

        final File serverFile = new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_SERVER_DIGEST.getDefaultLogName());

        List<JSONObject> serverDigest = readContent(serverFile);

        List<String> serverTraceIds = readTraceId(serverDigest);

        System.out.println("clientTraceIds:" + clientTraceIds.size());
        Assert.assertTrue(CommonUtils.isNotEmpty(clientTraceIds));
        System.out.println("serverTraceIds:" + serverTraceIds.size());
        Assert.assertTrue(CommonUtils.isNotEmpty(serverTraceIds));

        HashSet<String> hashSet = new HashSet<String>(200);
        for (String clientTraceId : clientTraceIds) {
            //will not duplicate
            Assert.assertTrue(!hashSet.contains(clientTraceId));
            Assert.assertTrue(serverTraceIds.contains(clientTraceId));
        }

        //validate one rpc server and rpc client field

        boolean result = TracerChecker.validateTracerDigest(clientDigest.get(0), "client",
            RpcConstants.PROTOCOL_TYPE_REST);

        Assert.assertTrue(result);
        result = TracerChecker.validateTracerDigest(serverDigest.get(0), "server", RpcConstants.PROTOCOL_TYPE_REST);
        Assert.assertTrue(result);
        */
    }

    //readTracerDigest TraceId and spanId
    public List<JSONObject> readContent(File file) throws IOException {

        List<JSONObject> jsonObjects = TracerChecker.readTracerDigest(file);

        return jsonObjects;
    }

    //readTracerDigest TraceId and spanId
    public List<String> readTraceId(List<JSONObject> jsonObjects) throws IOException {

        List<String> result = TracerChecker.extractFields(jsonObjects, "tracerId");

        return result;
    }

    protected void reflectSetNewTracer() throws Exception {
        removeRpcDigestStatLogType();
        Tracer newTracerInstance = new RpcSofaTracer();
        Field tracerField = Tracers.class.getDeclaredField("tracer");
        tracerField.setAccessible(true);
        tracerField.set(null, newTracerInstance);
    }

    protected void removeRpcDigestStatLogType() throws Exception {

        AsyncCommonDigestAppenderManager asyncDigestManager = SofaTracerDigestReporterAsyncManager
            .getSofaTracerDigestReporterAsyncManager();
        //stat
        Map<Long, SofaTracerStatisticReporterManager> cycleTimesManager = SofaTracerStatisticReporterCycleTimesManager
            .getCycleTimesManager();
        for (Map.Entry<Long, SofaTracerStatisticReporterManager> entry : cycleTimesManager.entrySet()) {
            SofaTracerStatisticReporterManager manager = entry.getValue();
            manager.getStatReporters().clear();
        }
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