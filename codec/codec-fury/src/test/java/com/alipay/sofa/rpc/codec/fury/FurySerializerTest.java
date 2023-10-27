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
package com.alipay.sofa.rpc.codec.fury;

import java.util.*;

import com.alipay.sofa.rpc.codec.fury.model.blacklist.NotRegister;
import com.alipay.sofa.rpc.codec.fury.model.Registered.DemoRequest;
import com.alipay.sofa.rpc.codec.fury.model.Registered.DemoResponse;
import com.alipay.sofa.rpc.codec.fury.model.whitelist.DemoService;
import com.alipay.sofa.rpc.codec.fury.model.Registered.RegisteredClass;
import org.junit.Assert;
import org.junit.Test;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;

/**
 * @author lipan
 */
public class FurySerializerTest {

    private final FurySerializer serializer = new FurySerializer();

    public FurySerializerTest() {
    }

    @Test
    public void create() {
    }

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
        Date object = new Date();
        try {
            AbstractByteBuf encode = serializer.encode(object, null);
            Assert.assertEquals(object, serializer.decode(encode, Date.class, null));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(!error);

        error = false;
        try {
            serializer.encode(1, null);
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
    public void testChecker() throws Exception {
        boolean error = false;
        RegisteredClass registeredClass = new RegisteredClass();
        System.out.println();
        try {
            // registered this class
            serializer.encode(registeredClass, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        error = false;
        NotRegister notRegister = new NotRegister();
        System.out.println();
        try {
            //Not registered this class
            serializer.encode(notRegister, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;

        //  test add or delete

        serializer.addWhiteList("com.alipay.sofa.rpc.codec.fury.model.blacklist.*");
        try {
            //Not registered this class
            serializer.encode(notRegister, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        serializer.addBlackList("com.alipay.sofa.rpc.codec.fury.model.blacklist.*");
        try {
            //Not registered this class
            serializer.encode(notRegister, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

    }

    @Test
    public void testSofaRequest() throws Exception {
        SofaRequest request = buildRequest();
        AbstractByteBuf data = serializer.encode(request, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        error = false;
        try {
            serializer.decode(data, new SofaRequest(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

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

        // null request
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newRequest = new SofaRequest();
        serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, head);
        final Object[] methodArgs = newRequest.getMethodArgs();
        Assert.assertEquals(null, ((DemoRequest) methodArgs[0]).getName());
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
        Assert.assertFalse(error);

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

        // null response
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, DemoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "say");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newResponse = new SofaResponse();
        serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertNotNull(newResponse.getAppResponse());
        Assert.assertEquals(null, ((DemoResponse) newResponse.getAppResponse()).getWord());

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

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(DemoService.class.getName());
        request.setMethodName("say");
        request.setMethod(DemoService.class.getMethod("say", DemoRequest.class));
        final DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("name");
        request.setMethodArgs(new Object[] { demoRequest });
        request.setMethodArgSigs(new String[] { DemoRequest.class.getCanonicalName() });
        request.setTargetServiceUniqueName(DemoService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 20);
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

}