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
package com.alipay.sofa.rpc.event;

import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;
import io.opentracing.tag.Tags;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_APP_NAME;
import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_INVOKE_TYPE;
import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_PROTOCOL;

/**
 * @author Even
 * @date 2024/2/27 19:48
 */
public class SofaTracerSubscriberTest {

    @BeforeClass
    public static void beforeClass() {
        System.getProperties().put(RpcOptions.DEFAULT_TRACER, "sofaTracer");
    }

    @Test
    public void testClientSendAndServerReceiveTracerEvent() {
        SofaRequest sofaRequest = new SofaRequest();
        sofaRequest.setMethodName("testService");
        sofaRequest.setTimeout(1000);
        sofaRequest.setInvokeType("sync");
        sofaRequest.setTargetServiceUniqueName("testInterface:1.0");
        sofaRequest.setTargetAppName("targetAppName");
        EventBus.post(new ClientStartInvokeEvent(sofaRequest));
        SofaTracerSpan currentClientSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Map<String, String> clientTagsWithStr = currentClientSpan.getTagsWithStr();
        Assert.assertEquals("client", clientTagsWithStr.get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals(sofaRequest.getTargetServiceUniqueName(), clientTagsWithStr.get(RpcSpanTags.SERVICE));
        Assert.assertEquals(sofaRequest.getMethodName(), clientTagsWithStr.get(RpcSpanTags.METHOD));
        Assert.assertEquals(Thread.currentThread().getName(), clientTagsWithStr.get(RpcSpanTags.CURRENT_THREAD_NAME));

        Assert.assertNull(sofaRequest.getRequestProps());
        EventBus.post(new ClientBeforeSendEvent(sofaRequest));
        Map traceContext = (Map) sofaRequest.getRequestProps().get(RemotingConstants.RPC_TRACE_NAME);
        Assert.assertNotNull(traceContext);

        sofaRequest.getRequestProps().put(HEAD_PROTOCOL, "tr");
        sofaRequest.getRequestProps().put(HEAD_INVOKE_TYPE, sofaRequest.getInvokeType());
        sofaRequest.getRequestProps().put(HEAD_APP_NAME, "callerAppName");
        RpcInternalContext.getContext().setRemoteAddress("127.0.0.1", 12200);
        EventBus.post(new ServerReceiveEvent(sofaRequest));
        SofaTracerSpan currentServerSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Map<String, String> serverTagsWithStr = currentServerSpan.getTagsWithStr();
        Assert.assertEquals("server", serverTagsWithStr.get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals(sofaRequest.getTargetServiceUniqueName(), serverTagsWithStr.get(RpcSpanTags.SERVICE));
        Assert.assertEquals(sofaRequest.getMethodName(), serverTagsWithStr.get(RpcSpanTags.METHOD));
        Assert.assertEquals(sofaRequest.getTargetAppName(), serverTagsWithStr.get(RpcSpanTags.LOCAL_APP));
        Assert.assertEquals(sofaRequest.getInvokeType(), serverTagsWithStr.get(RpcSpanTags.INVOKE_TYPE));
        Assert.assertEquals("tr", serverTagsWithStr.get(RpcSpanTags.PROTOCOL));
        Assert.assertEquals("127.0.0.1", serverTagsWithStr.get(RpcSpanTags.REMOTE_IP));
        Assert.assertEquals("callerAppName", serverTagsWithStr.get(RpcSpanTags.REMOTE_APP));
        SofaTraceContextHolder.getSofaTraceContext().clear();
    }

    @AfterClass
    public static void afterClass() {
        System.getProperties().remove(RpcOptions.DEFAULT_TRACER);
    }

}