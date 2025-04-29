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
package com.alipay.sofa.rpc.tracer.sofatracer.log.event;

import com.alipay.common.tracer.core.appender.builder.JsonStringBuilder;
import com.alipay.common.tracer.core.appender.encoder.SpanEncoder;
import com.alipay.common.tracer.core.context.span.SofaTracerSpanContext;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.util.Map;

/**
 * @author Even
 * @date 2025/3/19 19:44
 */
public abstract class AbstractRpcEventJsonEncoder implements SpanEncoder<SofaTracerSpan> {

    protected final JsonStringBuilder buffer = new JsonStringBuilder();

    public void appendSlot(SofaTracerSpan span) {
        SofaTracerSpanContext spanContext = span.getSofaTracerSpanContext();
        Map<String, String> spanTagsWithStr = span.getTagsWithStr();
        // traceId
        buffer.append(RpcSpanTags.TRACERID, spanContext.getTraceId());
        // spanId
        buffer.append(RpcSpanTags.SPANID, spanContext.getSpanId());
        // fromApp
        buffer.append(RpcSpanTags.LOCAL_APP, spanTagsWithStr.get(RpcSpanTags.LOCAL_APP));
        // toApp
        buffer.append(RpcSpanTags.REMOTE_APP, spanTagsWithStr.get(RpcSpanTags.REMOTE_APP));
        // event tags
        Map<String, String> tagsWithStr = span.getEventData().getEventTagWithStr();
        if (CommonUtils.isNotEmpty(tagsWithStr)) {
            for (Map.Entry<String, String> entry : tagsWithStr.entrySet()) {
                buffer.append(entry.getKey(), entry.getValue());
            }
        }
        Map<String, Number> tagsWithNumber = span.getEventData().getEventTagWithNumber();
        if (CommonUtils.isNotEmpty(tagsWithNumber)) {
            for (Map.Entry<String, Number> entry : tagsWithNumber.entrySet()) {
                Number value = entry.getValue();
                buffer.append(entry.getKey(), value == null ? null : String.valueOf(value));
            }
        }
        Map<String, Boolean> tagsWithBool = span.getEventData().getEventTagWithBool();
        if (CommonUtils.isNotEmpty(tagsWithBool)) {
            for (Map.Entry<String, Boolean> entry : tagsWithBool.entrySet()) {
                buffer.append(entry.getKey(), entry.getValue());
            }
        }
    }

}
