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
package com.alipay.sofa.rpc.codec.protostuff;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author leizhiyuan
 */
public class ProtostuffSerializerTest {

    ProtostuffSerializer serializer = new ProtostuffSerializer();

    @Test
    public void encodeAndDecode() {
        boolean error = false;
        try {
            serializer.encode(null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        ExampleObj req = new ExampleObj();
        req.setName("xxx");
        AbstractByteBuf byteBuf = serializer.encode(req, null);
        ExampleObj req2 = (ExampleObj) serializer.decode(byteBuf, ExampleObj.class, null);
        Assert.assertEquals(req.getName(), req2.getName());

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
        SofaRequest request = buildRequest();
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

        Map<String, String> head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, ProtostuffService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "echoStr");
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
        Assert.assertEquals("xxxx", ((ExampleObj) newRequest.getMethodArgs()[0]).getName());
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());
        Assert.assertEquals(newRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            request.getRequestProp(RemotingConstants.RPC_TRACE_NAME));

        // null request
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, ProtostuffService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "echoStr");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newRequest = new SofaRequest();
        serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, head);
        Assert.assertNull(((ExampleObj) newRequest.getMethodArgs()[0]).getName());
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
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, ProtostuffService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "echoStr");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        response = new SofaResponse();
        final ExampleObj exampleObj = new ExampleObj();
        exampleObj.setName("result");
        response.setAppResponse(exampleObj);
        data = serializer.encode(response, null);
        SofaResponse newResponse = new SofaResponse();
        serializer.decode(data, newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
        Assert.assertEquals("result", ((ExampleObj) newResponse.getAppResponse()).getName());

        // null response
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, ProtostuffService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "echoStr");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newResponse = new SofaResponse();
        serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertNotNull(newResponse.getAppResponse());
        Assert.assertNull(((ExampleObj) newResponse.getAppResponse()).getName());

        // error response
        head = new HashMap<String, String>();
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, ProtostuffService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "echoStr");
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

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(ProtostuffService.class.getName());
        request.setMethodName("echoStr");
        request.setMethod(ProtostuffService.class.getMethod("echoStr", ExampleObj.class));
        ExampleObj exampleObj = new ExampleObj();
        exampleObj.setName("xxxx");
        request.setMethodArgs(new Object[] { exampleObj });
        request.setMethodArgSigs(new String[] { ExampleObj.class.getCanonicalName() });
        request.setTargetServiceUniqueName(ProtostuffService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 11);
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