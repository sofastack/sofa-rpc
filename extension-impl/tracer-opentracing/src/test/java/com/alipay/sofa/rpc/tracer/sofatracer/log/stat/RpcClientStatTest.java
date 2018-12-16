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
package com.alipay.sofa.rpc.tracer.sofatracer.log.stat;

import com.alipay.common.tracer.core.SofaTracer;
import com.alipay.common.tracer.core.reporter.facade.Reporter;
import com.alipay.common.tracer.core.reporter.stat.model.StatKey;
import com.alipay.common.tracer.core.reporter.stat.model.StatMapKey;
import com.alipay.common.tracer.core.reporter.stat.model.StatValues;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.RpcSofaTracer;
import com.alipay.sofa.rpc.tracer.sofatracer.base.AbstractTracerBase;
import com.alipay.sofa.rpc.tracer.sofatracer.factory.MemoryReporterImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class RpcClientStatTest extends AbstractTracerBase {

    private MemoryReporterImpl memoryReporter;

    @Before
    public void before() {

        System.setProperty("stat_log_interval", "1");
        System.setProperty("reporter_type", "MEMORY");
        try {
            reflectSetNewTracer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testClientStat() {

        try {
            Tracer rpcSofaTracer = Tracers.getTracer();

            Field tracerField = null;
            try {
                tracerField = RpcSofaTracer.class.getDeclaredField("sofaTracer");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            tracerField.setAccessible(true);

            SofaTracer tracer = null;
            //OpenTracing tracer 标准实现
            try {
                tracer = (SofaTracer) tracerField.get(rpcSofaTracer);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            Reporter clientReporter = tracer.getClientReporter();
            assertNotNull(clientReporter);
            assertTrue(clientReporter instanceof MemoryReporterImpl);

            memoryReporter = (MemoryReporterImpl) clientReporter;

            final SofaRequest request = new SofaRequest();
            request.setInterfaceName("a");
            request.setTargetServiceUniqueName("app.service:1.0");
            request.setMethodName("method");

            RpcInternalContext context = RpcInternalContext.getContext();
            context.setAttachment(RpcConstants.INTERNAL_KEY_APP_NAME, "client");

            //this will not be used, only in real invoke
            final ProviderInfo providerInfo = new ProviderInfo();
            providerInfo.setStaticAttr(ProviderInfoAttrs.ATTR_APP_NAME, "server");
            context.setProviderInfo(providerInfo);

            for (int i = 0; i < 10; i++) {
                rpcSofaTracer.startRpc(request);

                rpcSofaTracer.clientBeforeSend(request);

                final SofaResponse response = new SofaResponse();
                response.setAppResponse("b");
                rpcSofaTracer.clientReceived(request, response, null);
            }

            Map<StatKey, StatValues> datas = memoryReporter.getStoreDatas();

            LOGGER.info("1" + datas);

            Assert.assertEquals(1, datas.size());

            for (Map.Entry entry : datas.entrySet()) {
                final StatMapKey key = (StatMapKey) entry.getKey();
                final StatValues value = (StatValues) entry.getValue();

                Assert.assertEquals("client,,app.service:1.0,method", key.getKey());
                Assert.assertEquals(10, value.getCurrentValue()[0]);
            }
            request.setTargetServiceUniqueName("app.service:2.0");

            for (int i = 0; i < 20; i++) {
                rpcSofaTracer.startRpc(request);

                rpcSofaTracer.clientBeforeSend(request);

                final SofaResponse response = new SofaResponse();
                response.setAppResponse("b");
                rpcSofaTracer.clientReceived(request, response, null);
            }

            LOGGER.info("2" + datas);

            int i = 0;
            for (Map.Entry entry : datas.entrySet()) {

                if (i == 0) {
                    continue;
                }
                final StatMapKey key = (StatMapKey) entry.getKey();
                final StatValues value = (StatValues) entry.getValue();

                Assert.assertEquals("client,,app.service:2.0,method", key.getKey());
                Assert.assertEquals(20, value.getCurrentValue()[0]);
            }

            Assert.assertEquals(2, datas.size());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    @After
    public void afterClientClass() {
        System.setProperty("reporter_type", "DISK");

        if (memoryReporter != null) {
            memoryReporter.clearAll();
        }
    }
}