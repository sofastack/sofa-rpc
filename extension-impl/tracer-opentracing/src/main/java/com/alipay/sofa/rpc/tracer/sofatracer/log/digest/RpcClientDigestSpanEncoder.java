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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.io.IOException;
import java.util.Map;

/**
 * Encode RpcClientDigestSpan to normal string
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcClientDigestSpanEncoder implements SpanEncoder<SofaTracerSpan> {

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
        //目标地址
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_IP));
        //目标系统名
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_APP));
        //目标 Zone
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_ZONE));
        //目标 IDC
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_IDC));
        //目标 City
        xsb.append(tagsWithStr.get(RpcSpanTags.REMOTE_CITY));
        //uid
        xsb.append(tagsWithStr.get(RpcSpanTags.USER_ID));
        //结果码（00=成功/01=业务异常/02=RPC逻辑错误/03=超时失败/04=路由失败）
        xsb.append(tagsWithStr.get(RpcSpanTags.RESULT_CODE));
        //请求大小（单位byte）
        long reqSize = CommonUtils.parseNum((Integer) tagsWithNumber.get(RpcSpanTags.REQ_SIZE), 0);
        xsb.append(reqSize + SofaTracerConstant.BYTE);
        //响应大小（单位byte，WS 的这个值为永远为 -1B，可以忽略，-1 这个值来自于 Response 的 Content-Length 头）
        int respSize = CommonUtils.parseNum((Integer) tagsWithNumber.get(RpcSpanTags.RESP_SIZE), 0);
        xsb.append(respSize + SofaTracerConstant.BYTE);
        //调用耗时（ms）
        long duration = span.getEndTime() - span.getStartTime();
        xsb.append(duration + SofaTracerConstant.MS);
        //链接建立耗时（ms）
        long connectionTime = CommonUtils.parseNum((Long) tagsWithNumber.get(RpcSpanTags.CLIENT_CONN_TIME), 0L);
        xsb.append(connectionTime + SofaTracerConstant.MS);
        //请求序列化耗时（ms）
        int reqSerTime = CommonUtils.parseNum((Integer) tagsWithNumber.get(RpcSpanTags.REQ_SERIALIZE_TIME), 0);
        xsb.append(reqSerTime + SofaTracerConstant.MS);
        //超时参考耗时（ms）（目前，此字段的值 = 调用耗时 - 链接建立耗时，它的含义是当出现调用超时的时候，这个值能够比调用耗时更加准确的反映本次请求的调用超时时间到底是多少。）
        xsb.append((duration - connectionTime) + SofaTracerConstant.MS);
        //当前线程名
        xsb.append(tagsWithStr.get(RpcSpanTags.CURRENT_THREAD_NAME));
        //路由记录，路由选择的过程记录
        xsb.append(tagsWithStr.get(RpcSpanTags.ROUTE_RECORD));
        //eid（弹性数据位）
        xsb.append(tagsWithStr.get(RpcSpanTags.ELASTIC_ID));
        //elastic（表明本次调用的服务是否需要弹性，值为“T”or“F”，此字段透传一次）TODO
        xsb.append(tagsWithStr.get(RpcSpanTags.BE_ELASTIC));
        //beElasticServiceName（表明这次调用是转发调用,转发的服务名称和方法名称是啥，值如：“com.test.service.testservice.TestService:1.0:biztest---doProcess”）
        xsb.append(tagsWithStr.get(RpcSpanTags.ELASTIC_SERVICE_NAME));
        //client IP
        xsb.append(tagsWithStr.get(RpcSpanTags.LOCAL_IP));
        //client 端口
        xsb.append(StringUtils.defaultString(tagsWithNumber.get(RpcSpanTags.LOCAL_PORT)));
        //当前 Zone
        xsb.append(tagsWithStr.get(RpcSpanTags.LOCAL_ZONE));
        //系统穿透数据（kv 格式，用于传送系统灾备信息等）
        xsb.append(baggageSystemSerialized(spanContext));
        //穿透数据放在最后
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
