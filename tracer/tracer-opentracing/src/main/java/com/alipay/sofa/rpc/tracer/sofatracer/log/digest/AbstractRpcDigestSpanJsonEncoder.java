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
package com.alipay.sofa.rpc.tracer.sofatracer.log.digest;

import com.alipay.common.tracer.core.appender.builder.JsonStringBuilder;
import com.alipay.common.tracer.core.appender.encoder.SpanEncoder;
import com.alipay.common.tracer.core.context.span.SofaTracerSpanContext;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.util.Map;

/**
 * Encode RpcClientDigestSpan to json string
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public abstract class AbstractRpcDigestSpanJsonEncoder implements SpanEncoder<SofaTracerSpan> {

    public void appendSlot(JsonStringBuilder jsb, SofaTracerSpan span) {
        SofaTracerSpanContext spanContext = span.getSofaTracerSpanContext();
        //traceId
        jsb.append(RpcSpanTags.TRACERID, spanContext.getTraceId());
        //spanId
        jsb.append(RpcSpanTags.SPANID, spanContext.getSpanId());
        //tags
        Map<String, String> tagsWithStr = span.getTagsWithStr();
        if (CommonUtils.isNotEmpty(tagsWithStr)) {
            for (Map.Entry<String, String> entry : tagsWithStr.entrySet()) {
                jsb.append(entry.getKey(), entry.getValue());
            }
        }
        Map<String, Number> tagsWithNumber = span.getTagsWithNumber();
        if (CommonUtils.isNotEmpty(tagsWithNumber)) {
            for (Map.Entry<String, Number> entry : tagsWithNumber.entrySet()) {
                Number value = entry.getValue();
                jsb.append(entry.getKey(), value == null ? null : String.valueOf(value));
            }
        }
        Map<String, Boolean> tagsWithBool = span.getTagsWithBool();
        if (CommonUtils.isNotEmpty(tagsWithBool)) {
            for (Map.Entry<String, Boolean> entry : tagsWithBool.entrySet()) {
                jsb.append(entry.getKey(), entry.getValue());
            }
        }
        //系统穿透数据（kv 格式，用于传送系统灾备信息等）
        jsb.append(RpcSpanTags.BAGGAGE, baggageSerialized(spanContext));
    }

    protected String baggageSerialized(SofaTracerSpanContext spanContext) {
        //业务 baggage
        return spanContext.getBizSerializedBaggage();
    }
}
