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
package com.alipay.sofa.rpc.filter;

import com.alipay.common.tracer.core.SofaTracer;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.common.tracer.core.utils.TracerUtils;
import com.alipay.sofa.rpc.common.MetadataHolder;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import io.grpc.Context;
import io.grpc.Metadata;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.alipay.sofa.rpc.filter.PressureMarkTransformFilter.PRESSURE;
import static com.alipay.sofa.rpc.server.triple.TripleHeadKeys.HEAD_KEY_TRAFFIC_TYPE;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class PressureMarkTransformFilterTest {

    public static final EmptyInvoker invoker = new EmptyInvoker(null);
    public static final SofaRequest  request = null;
    public static final SofaTracer   tracer  = new SofaTracer.Builder("TEST").build();

    @Before
    public void before() {
        SofaTracerSpan span = (SofaTracerSpan) tracer.buildSpan("test").start();
        SofaTraceContextHolder.getSofaTraceContext().push(span);
    }

    @After
    public void after() {
        SofaTraceContextHolder.getSofaTraceContext().clear();
    }

    @Test
    public void testConsumerPressure() {
        //consumer side
        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Map<String, String> bizBaggage = currentSpan.getSofaTracerSpanContext().getBizBaggage();
        bizBaggage.put("mark", "T");
        Assert.assertTrue(TracerUtils.isLoadTest(currentSpan));

        PressureMarkTransformFilter filter = new PressureMarkTransformFilter();
        filter.invoke(invoker, request);

        Assert.assertEquals(PRESSURE, invoker.getMetaHolder().get(HEAD_KEY_TRAFFIC_TYPE.name()));
    }

    @Test
    public void testNoConsumerPressure() {
        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Assert.assertFalse(TracerUtils.isLoadTest(currentSpan));

        PressureMarkTransformFilter filter = new PressureMarkTransformFilter();
        filter.invoke(invoker, request);

        Assert.assertNull(invoker.getMetaHolder().get(HEAD_KEY_TRAFFIC_TYPE.name()));
    }

    @Test
    public void testProviderPressure() {
        Metadata metadata = new Metadata();
        metadata.put(HEAD_KEY_TRAFFIC_TYPE, "pressure");
        Context context = Context.current().withValue(TracingContextKey.getKeyMetadata(), metadata);
        context.attach();

        PressureMarkTransformFilter filter = new PressureMarkTransformFilter();
        filter.invoke(invoker, request);

        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Assert.assertTrue(TracerUtils.isLoadTest(currentSpan));
    }

    @Test
    public void testNoProviderPressure() {
        Metadata metadata = new Metadata();
        Context context = Context.current().withValue(TracingContextKey.getKeyMetadata(), metadata);
        context.attach();

        PressureMarkTransformFilter filter = new PressureMarkTransformFilter();
        filter.invoke(invoker, request);

        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Assert.assertFalse(TracerUtils.isLoadTest(currentSpan));
    }

    static class EmptyInvoker extends FilterInvoker {

        public Map<String, String> getMetaHolder() {
            return metaHolder;
        }

        private Map<String, String> metaHolder;

        protected EmptyInvoker(AbstractInterfaceConfig config) {
            super(config);
        }

        @Override
        public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
            this.metaHolder = MetadataHolder.getMetaHolder();
            return null;
        }
    }

}