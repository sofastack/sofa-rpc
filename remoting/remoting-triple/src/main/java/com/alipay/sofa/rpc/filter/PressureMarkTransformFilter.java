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

import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.common.tracer.core.utils.TracerUtils;
import com.alipay.sofa.rpc.common.MetadataHolder;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import io.grpc.Metadata;

import java.util.Map;

import static com.alipay.sofa.rpc.server.triple.TripleHeadKeys.HEAD_KEY_TRAFFIC_TYPE;

/**
 * Transform triple/
 *
 * @author zhaowang
 * @version : PressureMarkTransformFilter.java, v 0.1 2020年09月09日 2:27 下午 zhaowang Exp $
 */
@Extension(value = "pressure")
@AutoActive(consumerSide = true, providerSide = true)
public class PressureMarkTransformFilter extends Filter {

    public static final String PRESSURE = "pressure";
    public static final String MARK     = "mark";
    public static final String T        = "T";

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        // consumer side, if in provider side,loadTest always false
        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        boolean loadTest = TracerUtils.isLoadTest(currentSpan);
        if (loadTest) {
            Map<String, String> metaHolder = MetadataHolder.getMetaHolder();
            metaHolder.put(HEAD_KEY_TRAFFIC_TYPE.name(), PRESSURE);
        }

        // provider side ,if in consumer side, metadata == null
        Metadata metadata = TracingContextKey.getKeyMetadata().get();
        if (metadata != null) {
            String s = metadata.get(HEAD_KEY_TRAFFIC_TYPE);
            if (PRESSURE.equals(s)) {
                currentSpan.getSofaTracerSpanContext().setBizBaggageItem(MARK, T);
            }
        }
        try {
            return invoker.invoke(request);
        } finally {
            MetadataHolder.clear();
        }
    }
}