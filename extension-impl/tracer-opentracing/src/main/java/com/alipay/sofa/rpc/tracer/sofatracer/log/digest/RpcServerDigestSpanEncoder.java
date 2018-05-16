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

import com.alipay.common.tracer.core.appender.builder.XStringBuilder;
import com.alipay.common.tracer.core.appender.encoder.SpanEncoder;
import com.alipay.common.tracer.core.appender.self.Timestamp;
import com.alipay.common.tracer.core.constants.SofaTracerConstant;
import com.alipay.common.tracer.core.context.span.SofaTracerSpanContext;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.io.IOException;
import java.util.Map;

/**
 * Encode RpcServerDigestSpan to normal string
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcServerDigestSpanEncoder implements SpanEncoder<SofaTracerSpan> {

    private XStringBuilder xsb = new XStringBuilder();

    @Override
    public String encode(SofaTracerSpan span) throws IOException {
        xsb.reset();
        //打印时间
        xsb.append(Timestamp.format(span.getEndTime()));
        //添加其他字段
        this.appendSlot(xsb, span);
        return xsb.toString();
    }

    public void appendSlot(XStringBuilder xsb, SofaTracerSpan span) {
        SofaTracerSpanContext spanContext = span.getSofaTracerSpanContext();
        //tags
        Map<String, String> tagsWithStr = span.getTagsWithStr();
        //时间和大小相关
        Map<String, Number> tagsWithNumber = span.getTagsWithNumber();
        //app
        xsb.append(tagsWithStr.get(RpcSpanTags.LOCAL_APP));
        //traceId
        xsb.append(spanContext.getTraceId());
        //spanId
        xsb.append(spanContext.getSpanId());
        //service name
        xsb.append(tagsWithStr.get(RpcSpanTags.SERVICE));
        //method name
        xsb.append(tagsWithStr.get(RpcSpanTags.METHOD));
        //protocol
        xsb.append(tagsWithStr.get(RpcSpanTags.PROTOCOL));
        //调用方式
        xsb.append(tagsWithStr.get(RpcSpanTags.INVOKE_TYPE));
        //服务端特有开始
        //调用者 URL
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_IP));
        //调用者应用名
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_APP));
        //调用者 Zone
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_ZONE));
        //调用者 IDC
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_IDC));
        //服务端特有结束
        //请求处理耗时（ms）
        long duration = span.getEndTime() - span.getStartTime();
        xsb.append(duration + SofaTracerConstant.MS);
        //服务端响应序列化耗时（ms）
        long respSerTime = CommonUtils.parseNum((Integer) tagsWithNumber.get(RpcSpanTags.RESP_SERIALIZE_TIME), 0);
        xsb.append(respSerTime + SofaTracerConstant.MS);
        //当前线程名
        xsb.append(tagsWithStr.get(RpcSpanTags.CURRENT_THREAD_NAME));
        //结果码（00=成功/01=业务异常/02=RPC逻辑错误）
        xsb.append(tagsWithStr.get(RpcSpanTags.RESULT_CODE));
        //beElasticServiceName（表明这次调用是转发调用,转发的服务名称和方法名称是啥值如：“com.test.service.testservice.TestService:1.0:biztest---doProcess”）
        xsb.append(tagsWithStr.get(RpcSpanTags.ELASTIC_SERVICE_NAME));
        //beElastic（表示没有被转发的处理）被转发服务端标示，"T"表示本次服务被转发
        xsb.append(tagsWithStr.get(RpcSpanTags.BE_ELASTIC));
        //rpc线程池等待时间
        long threadTime = CommonUtils.parseNum((Long) tagsWithNumber.get(RpcSpanTags.SERVER_THREAD_POOL_WAIT_TIME), 0L);
        xsb.append(threadTime + SofaTracerConstant.MS);
        //系统穿透数据（kv 格式，用于传送系统灾备信息等）
        xsb.append(baggageSystemSerialized(spanContext));
        //穿透数据（kv格式）放在最后
        xsb.appendEnd(this.baggageSerialized(spanContext));
    }

    /***
     * 系统透传数据
     * @param spanContext span 上下文
     * @return String
     */
    protected String baggageSystemSerialized(SofaTracerSpanContext spanContext) {
        //系统 baggage
        return spanContext.getSysSerializedBaggage();
    }

    protected String baggageSerialized(SofaTracerSpanContext spanContext) {
        //业务 baggage
        return spanContext.getBizSerializedBaggage();
    }
}
