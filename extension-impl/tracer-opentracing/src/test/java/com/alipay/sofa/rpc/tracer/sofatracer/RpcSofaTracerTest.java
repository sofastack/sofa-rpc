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
package com.alipay.sofa.rpc.tracer.sofatracer;

import com.alipay.common.tracer.core.SofaTracer;
import com.alipay.common.tracer.core.appender.manager.AsyncCommonDigestAppenderManager;
import com.alipay.common.tracer.core.context.trace.SofaTraceContext;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.reporter.digest.DiskReporterImpl;
import com.alipay.common.tracer.core.reporter.digest.manager.SofaTracerDigestReporterAsyncManager;
import com.alipay.common.tracer.core.reporter.facade.Reporter;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.tracer.sofatracer.base.AbstractTracerBase;
import com.alipay.sofa.rpc.tracer.sofatracer.log.stat.RpcClientStatJsonReporter;
import com.alipay.sofa.rpc.tracer.sofatracer.log.stat.RpcServerStatJsonReporter;
import com.alipay.sofa.rpc.tracer.sofatracer.log.type.RpcTracerLogEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * SofaTracer Tester.
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcSofaTracerTest extends AbstractTracerBase {

    private RpcSofaTracer rpcSofaTracer = new RpcSofaTracer();

    private SofaRequest   sofaRequest;

    @Before
    public void before() throws Exception {
        System.setProperty("reporter_type", "DISK");
        this.sofaRequest = new SofaRequest();
        try {
            reflectSetNewTracer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void after() throws Exception {
        System.setProperty("reporter_type", "DISK");
    }

    @Test
    public void testTracerInit() throws Exception {

        try {
            //注册 digest
            AsyncCommonDigestAppenderManager asyncDigestManager = SofaTracerDigestReporterAsyncManager
                .getSofaTracerDigestReporterAsyncManager();

            Field tracerField = RpcSofaTracer.class.getDeclaredField("sofaTracer");
            tracerField.setAccessible(true);
            //OpenTracing tracer 标准实现
            SofaTracer tracer = (SofaTracer) tracerField.get(this.rpcSofaTracer);
            assertTrue(tracer != null);
            Reporter clientReporter = tracer.getClientReporter();
            assertNotNull(clientReporter);
            assertTrue(clientReporter instanceof DiskReporterImpl);
            DiskReporterImpl clientDisk = (DiskReporterImpl) clientReporter;
            assertEquals(clientDisk.getDigestReporterType(), RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName());
            assertTrue(clientDisk.getStatReporter() instanceof RpcClientStatJsonReporter);
            //修改为 lazy 初始化了
            //assertFalse(asyncDigestManager.isAppenderAndEncoderExist(clientDisk.getDigestReporterType()));
            SofaRequest sofaRequest = new SofaRequest();
            rpcSofaTracer.startRpc(sofaRequest);
            rpcSofaTracer.clientBeforeSend(sofaRequest);
            rpcSofaTracer.clientReceived(sofaRequest, new SofaResponse(), null);
            //lazy 应该注册成功了
            assertTrue(asyncDigestManager.isAppenderAndEncoderExist(clientDisk.getDigestReporterType()));
            //print
            TimeUnit.SECONDS.sleep(1);
            Reporter serverReporter = tracer.getServerReporter();
            assertTrue(serverReporter instanceof DiskReporterImpl);
            assertNotNull(serverReporter);

            DiskReporterImpl serverDisk = (DiskReporterImpl) serverReporter;
            assertEquals(serverDisk.getDigestReporterType(), RpcTracerLogEnum.RPC_SERVER_DIGEST.getDefaultLogName());
            //assertFalse(asyncDigestManager.isAppenderAndEncoderExist(serverDisk.getDigestReporterType()));
            rpcSofaTracer.serverReceived(sofaRequest);
            rpcSofaTracer.serverSend(sofaRequest, new SofaResponse(), null);
            //print
            TimeUnit.SECONDS.sleep(1);
            assertTrue(asyncDigestManager.isAppenderAndEncoderExist(serverDisk.getDigestReporterType()));

            assertTrue(serverDisk.getStatReporter() instanceof RpcServerStatJsonReporter);
        } catch (Throwable e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Method: startRpc(SofaRequest request)
     */
    @Test
    public void testStartRpc() throws Exception {
        this.rpcSofaTracer.startRpc(sofaRequest);

        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        SofaTracerSpan sofaTracerSpan = sofaTraceContext.pop();
        assertNotNull(sofaTracerSpan);
        System.err.println("\n" + sofaTracerSpan);
    }
}
