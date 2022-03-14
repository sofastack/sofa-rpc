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
package com.alipay.sofa.rpc.codec.jackson;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alipay.sofa.rpc.codec.jackson.model.DemoRequest;
import com.alipay.sofa.rpc.codec.jackson.model.DemoRequest2;
import com.alipay.sofa.rpc.codec.jackson.model.DemoResponse;
import com.alipay.sofa.rpc.codec.jackson.model.DemoService;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class JacksonSerializerTest {

    JacksonSerializer serializer = new JacksonSerializer();

    @Test
    public void encodeAndDecode() {
        boolean error = false;
        try {
            serializer.encode(null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("a");
        AbstractByteBuf byteBuf = serializer.encode(demoRequest, null);
        DemoRequest req2 = (DemoRequest) serializer.decode(byteBuf, DemoRequest.class, null);
        Assert.assertEquals(demoRequest.getName(), req2.getName());

        AbstractByteBuf data = serializer.encode("xxx", null);
        String dst = (String) serializer.decode(data, String.class, null);
        Assert.assertEquals("xxx", dst);

        error = false;
        try {
            serializer.encode(new Date(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(!error);

        error = false;
        try {
            serializer.decode(data, null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, "", null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testSofaRequest() throws Exception {
        SofaRequest request = buildSayRequest();
        AbstractByteBuf data = serializer.encode(request, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, new SofaRequest(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // parameters size error
        Map<String, String> errorHead = new HashMap<String, String>();
        errorHead.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        errorHead.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        errorHead.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");

        try {
            SofaRequest say2Request = buildSay2Request();
            AbstractByteBuf say2Data = serializer.encode(say2Request, errorHead);
            serializer.decode(say2Data, new SofaRequest(), errorHead);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // success
        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put("unkown", "yes");

        SofaRequest newRequest = new SofaRequest();
        serializer.decode(data, newRequest, head);

        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertArrayEquals(newRequest.getMethodArgSigs(), request.getMethodArgSigs());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals("name", ((DemoRequest) newRequest.getMethodArgs()[0]).getName());
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());
        Assert.assertEquals(newRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            request.getRequestProp(RemotingConstants.RPC_TRACE_NAME));

    }

    @Test
    public void testSofaResponse() throws Exception {
        SofaResponse response = new SofaResponse();
        response.setAppResponse("1233");
        AbstractByteBuf data = serializer.encode(response, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaResponse.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, new SofaResponse(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // success response
        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        response = new SofaResponse();
        final DemoResponse response1 = new DemoResponse();
        response1.setWord("result");
        response.setAppResponse(response1);
        data = serializer.encode(response, null);
        SofaResponse newResponse = new SofaResponse();
        serializer.decode(data, newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
        Assert.assertEquals("result", ((DemoResponse) newResponse.getAppResponse()).getWord());

        // error response
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        response = new SofaResponse();
        response.setErrorMsg("1233");
        data = serializer.encode(response, null);
        newResponse = new SofaResponse();
        serializer.decode(data, newResponse, head);
        Assert.assertTrue(newResponse.isError());
        Assert.assertEquals(response.getErrorMsg(), newResponse.getErrorMsg());
    }

    @Test
    public void testListResponse() {
        // success response
        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say3");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        SofaResponse response = new SofaResponse();
        final DemoResponse response1 = new DemoResponse();
        response1.setWord("result");

        List<DemoResponse> listResponse = new ArrayList<DemoResponse>();
        listResponse.add(response1);

        response.setAppResponse(listResponse);

        AbstractByteBuf data = serializer.encode(response, null);
        SofaResponse newResponse = new SofaResponse();
        serializer.decode(data, newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
        Assert.assertEquals("result", ((List<DemoResponse>) newResponse.getAppResponse()).get(0).getWord());

    }

    @Test
    public void testMoreParameters() throws NoSuchMethodException {
        SofaRequest request = buildSay2Request();
        AbstractByteBuf data = serializer.encode(request, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, new SofaRequest(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;

        // parameters size error
        Map<String, String> errorHead1 = new HashMap<String, String>();
        errorHead1.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        errorHead1.put(RemotingConstants.HEAD_METHOD_NAME, "say2");

        try {
            ObjectMapper mapper = new ObjectMapper();
            DemoRequest req = new DemoRequest();
            req.setName("123");
            serializer
                .decode(new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(req)), new SofaRequest(), errorHead1);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // parameters size error
        Map<String, String> errorHead2 = new HashMap<String, String>();
        errorHead2.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        errorHead2.put(RemotingConstants.HEAD_METHOD_NAME, "say2");
        try {
            ObjectMapper mapper = new ObjectMapper();
            DemoRequest req = new DemoRequest();
            req.setName("123");
            serializer.decode(new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(new Object[] { req, "123" })),
                new SofaRequest(),
                errorHead2);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say2");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put("unkown", "yes");

        SofaRequest newRequest = new SofaRequest();
        serializer.decode(data, newRequest, head);

        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals("name", ((DemoRequest) newRequest.getMethodArgs()[0]).getName());
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());
        Assert.assertEquals(newRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            request.getRequestProp(RemotingConstants.RPC_TRACE_NAME));

    }

    @Test
    public void testListParameter() throws NoSuchMethodException {
        SofaRequest request = buildSay3Request();
        AbstractByteBuf data = serializer.encode(request, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            serializer.decode(data, new SofaRequest(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;

        // parameters size error
        Map<String, String> errorHead1 = new HashMap<String, String>();
        errorHead1.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        errorHead1.put(RemotingConstants.HEAD_METHOD_NAME, "say3");

        try {
            ObjectMapper mapper = new ObjectMapper();
            DemoRequest req = new DemoRequest();
            req.setName("123");
            serializer
                .decode(new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(req)), new SofaRequest(), errorHead1);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // parameters size error
        Map<String, String> errorHead2 = new HashMap<String, String>();
        errorHead2.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        errorHead2.put(RemotingConstants.HEAD_METHOD_NAME, "say3");
        try {
            ObjectMapper mapper = new ObjectMapper();
            DemoRequest req = new DemoRequest();
            req.setName("123");
            serializer.decode(new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(new Object[] { req, "123" })),
                new SofaRequest(),
                errorHead2);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say3");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put("unkown", "yes");

        SofaRequest newRequest = new SofaRequest();
        serializer.decode(data, newRequest, head);

        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals("name", ((List<DemoRequest>) newRequest.getMethodArgs()[0]).get(0).getName());
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());
        Assert.assertEquals(newRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            request.getRequestProp(RemotingConstants.RPC_TRACE_NAME));

    }

    private SofaRequest buildSayRequest() throws NoSuchMethodException {
        final DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("name");
        return buildRequest("say", new Object[] { demoRequest });
    }

    private SofaRequest buildSay2Request() throws NoSuchMethodException {
        final DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("name");

        Map<String, String> ctx = new HashMap<String, String>();
        ctx.put("abc", "123");

        return buildRequest("say2", new Object[] { demoRequest, ctx, 123 });
    }

    private SofaRequest buildSay3Request() throws NoSuchMethodException {
        final DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("name");

        List<DemoRequest> list = new ArrayList<DemoRequest>();
        list.add(demoRequest);

        return buildRequest("say3", new Object[] { list });
    }

    private SofaRequest buildRequest(String methodName, Object[] args) throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(DemoService.class.getName());
        request.setMethodName(methodName);
        Method method = null;
        for (Method m : DemoService.class.getMethods()) {
            if (m.getName().equals(methodName)) {
                method = m;
            }
        }
        request.setMethod(method);
        request.setMethodArgs(args);
        List<String> argSigs = new ArrayList<String>();
        for (Object req : args) {
            argSigs.add(req.getClass().getName());
        }
        request.setMethodArgSigs(argSigs.toArray(new String[argSigs.size()]));
        request.setTargetServiceUniqueName(DemoService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 12);
        request.setTimeout(1024);
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "xxx");
        map.put("b", "yyy");
        request.addRequestProp(RemotingConstants.RPC_TRACE_NAME, map);
        request.setSofaResponseCallback(new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {

            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {

            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {

            }
        });
        return request;
    }

    @Test
    public void testJacksonFeature() throws UnsupportedEncodingException {

        try {
            JacksonSerializer serializer = new JacksonSerializer();
            serializer.decode(new ByteArrayWrapperByteBuf("{\"a\":1}".getBytes("UTF-8")), DemoRequest.class, null);
            Assert.fail();
        } catch (SofaRpcException e) {
            // ok
        } catch (Throwable e) {
            Assert.fail();
        }

        try {
            JacksonSerializer serializer = new JacksonSerializer();
            serializer.encode(new DemoRequest2(), null);
            Assert.fail();
        } catch (SofaRpcException e) {
            // ok
        } catch (Throwable e) {
            Assert.fail();
        }

        System.setProperty("sofa.rpc.codec.jackson.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES", "false");
        JacksonSerializer serializer = new JacksonSerializer();
        serializer.decode(new ByteArrayWrapperByteBuf("{\"a\":1}".getBytes("UTF-8")), DemoRequest.class, null);

        System.setProperty("sofa.rpc.codec.jackson.SerializationFeature.FAIL_ON_EMPTY_BEANS", "false");
        serializer = new JacksonSerializer();
        serializer.encode(new DemoRequest2(), null);

        System.setProperty("sofa.rpc.codec.jackson.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES", "true");
        System.setProperty("sofa.rpc.codec.jackson.SerializationFeature.FAIL_ON_EMPTY_BEANS", "true");

    }

}