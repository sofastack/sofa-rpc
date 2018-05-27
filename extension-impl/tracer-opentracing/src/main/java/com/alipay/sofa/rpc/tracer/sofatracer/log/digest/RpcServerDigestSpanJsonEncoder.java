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
import com.alipay.common.tracer.core.appender.self.Timestamp;
import com.alipay.common.tracer.core.context.span.SofaTracerSpanContext;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.utils.CommonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Encode RpcServerDigestSpan to json string
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcServerDigestSpanJsonEncoder implements SpanEncoder<SofaTracerSpan> {

    private JsonStringBuilder jsb = new JsonStringBuilder();

    @Override
    public String encode(SofaTracerSpan span) throws IOException {
        jsb.reset();
        //打印时间
        jsb.appendBegin("timestamp", Timestamp.format(span.getEndTime()));
        //添加其他字段
        this.appendSlot(jsb, span);
        jsb.appendEnd();
        return jsb.toString();
    }

    public void appendSlot(JsonStringBuilder xsb, SofaTracerSpan span) {
        SofaTracerSpanContext spanContext = span.getSofaTracerSpanContext();
        //traceId
        jsb.append("tracerId", spanContext.getTraceId());
        //spanId
        jsb.append("spanId", spanContext.getSpanId());
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
        jsb.append("baggage", baggageSerialized(spanContext));
    }

    protected String baggageSerialized(SofaTracerSpanContext spanContext) {
        //业务 baggage
        return spanContext.getBizSerializedBaggage();
    }
}
