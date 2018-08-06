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
package jackson;

import com.alipay.sofa.rpc.codec.jackson.JacksonSerializer;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JacksonSerializer Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>七月 29, 2018</pre>
 */
public class JacksonSerializerTest {
    JacksonSerializer jacksonSerializer = new JacksonSerializer();

    /**
     * Method: encode(Object object, Map<String, String> context)
     */
    @Test
    public void testEncode() throws Exception {
        //序列化null
        boolean error = false;
        try {
            jacksonSerializer.encode(null, null);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        //序列化字符串
        AbstractByteBuf byteBuf = jacksonSerializer.encode("xxxyyy", null);
        String res = (String) jacksonSerializer.decode(byteBuf, String.class, null);
        Assert.assertEquals("xxxyyy", res);

        //序列化对象
        Echo echo = new Echo();
        echo.setAge(1);
        echo.setDel(false);
        echo.setName("name");
        Echo2 echo2 = new Echo2("1111");
        echo.setEcho2(echo2);
        AbstractByteBuf echoByteBuf = jacksonSerializer.encode(echo, null);
        Echo echoRes = (Echo) jacksonSerializer.decode(echoByteBuf, Echo.class, null);
        assert echoRes != null;
        Assert.assertEquals(echoRes.getName(), "name");
        Assert.assertEquals(echoRes.isDel(), false);
        Assert.assertEquals(echoRes.getAge(), 1);
        Assert.assertEquals(echoRes.getEcho2().getName(), "1111");

        //序列化日期
        Date date = new Date();
        AbstractByteBuf dateByteBuf = jacksonSerializer.encode(date, null);
        Date dateRes = (Date) jacksonSerializer.decode(dateByteBuf, Date.class, null);
        Assert.assertEquals(dateRes.getTime(), date.getTime());

        SofaRequest sofaRequest = new SofaRequest();
        sofaRequest.setInterfaceName("EchoService");
    }

    @Test
    public void testSofaRequest() throws NoSuchMethodException {
        SofaRequest request = buildRequest();
        AbstractByteBuf data = jacksonSerializer.encode(request, null);
        boolean error = false;
        try {
            jacksonSerializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            jacksonSerializer.decode(data, new SofaRequest(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Map<String, String> head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put("unkown", "yes");

        SofaRequest newRequest = new SofaRequest();
        jacksonSerializer.decode(data, newRequest, head);

        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertArrayEquals(newRequest.getMethodArgSigs(), request.getMethodArgSigs());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals("jackson.Echo", newRequest.getMethodArgSigs()[0]);
        Assert.assertEquals("111", ((Echo) newRequest.getMethodArgs()[0]).getName());
        Assert.assertEquals("test", newRequest.getMethodArgs()[1]);
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());
        Assert.assertEquals(newRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            request.getRequestProp(RemotingConstants.RPC_TRACE_NAME));

        // null request
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newRequest = new SofaRequest();
        jacksonSerializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, head);
        Assert.assertTrue(((Echo) newRequest.getMethodArgs()[0]).getName() == null);

        //Exception HEAD_TARGET_SERVICE is null
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, null);
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newRequest = new SofaRequest();
        error = false;
        try {
            jacksonSerializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, head);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        //Exception HEAD_METHOD_NAME is null
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, null);
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newRequest = new SofaRequest();
        error = false;
        try {
            jacksonSerializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, head);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

    }

    @Test
    public void testSofaResponse() throws Exception {
        SofaResponse response = new SofaResponse();
        response.setAppResponse("1233");
        AbstractByteBuf data = jacksonSerializer.encode(response, null);
        boolean error = false;
        try {
            jacksonSerializer.decode(data, SofaResponse.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            jacksonSerializer.decode(data, null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            jacksonSerializer.decode(data, new SofaResponse(), null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // success response
        Map<String, String> head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.HEAD_TARGET_APP, "targetApp");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        response = new SofaResponse();
        Echo2 echo2 = new Echo2();
        echo2.setDel(true);
        echo2.setName("123");
        echo2.setAge(18);
        response.setAppResponse(echo2);
        data = jacksonSerializer.encode(response, null);
        SofaResponse newResponse = new SofaResponse();
        jacksonSerializer.decode(data, newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals("123", ((Echo2) newResponse.getAppResponse()).getName());
        Assert.assertEquals(18, ((Echo2) newResponse.getAppResponse()).getAge());
        Assert.assertEquals(true, ((Echo2) newResponse.getAppResponse()).isDel());

        // null response
        head = new HashMap<String, String>(8);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        newResponse = new SofaResponse();
        jacksonSerializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newResponse, head);
        Assert.assertFalse(newResponse.isError());
        Assert.assertNotNull(newResponse.getAppResponse());
        assert ((Echo2) newResponse.getAppResponse()).getName() == null;

        // error response
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        response = new SofaResponse();
        response.setErrorMsg("1233");
        data = jacksonSerializer.encode(response, null);
        newResponse = new SofaResponse();
        jacksonSerializer.decode(data, newResponse, head);
        Assert.assertTrue(newResponse.isError());
        Assert.assertEquals(response.getErrorMsg(), newResponse.getErrorMsg());

        //throwable response
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        response = new SofaResponse();
        response.setAppResponse(new RuntimeException("test"));
        data = jacksonSerializer.encode(response, null);
        newResponse = new SofaResponse();
        jacksonSerializer.decode(data, newResponse, head);
        Assert.assertTrue(newResponse.isError());
        Assert.assertEquals("test", newResponse.getErrorMsg());

        //Exception:HEAD_TARGET_SERVICE is null
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, null);
        head.put(RemotingConstants.HEAD_METHOD_NAME, "checkOut");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        response = new SofaResponse();
        response.setAppResponse(new RuntimeException("test"));
        data = jacksonSerializer.encode(response, null);
        newResponse = new SofaResponse();
        error = false;
        try {
            jacksonSerializer.decode(data, newResponse, head);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        //Exception:HEAD_METHOD_NAME is null
        head = new HashMap<String, String>(10);
        head.put(RemotingConstants.HEAD_TARGET_SERVICE, EchoService.class.getCanonicalName() + ":1.0");
        head.put(RemotingConstants.HEAD_METHOD_NAME, null);
        head.put(RemotingConstants.RPC_TRACE_NAME + ".a", "xxx");
        head.put(RemotingConstants.RPC_TRACE_NAME + ".b", "yyy");
        head.put(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        response = new SofaResponse();
        response.setAppResponse(new RuntimeException("test"));
        data = jacksonSerializer.encode(response, null);
        newResponse = new SofaResponse();
        error = false;
        try {
            jacksonSerializer.decode(data, newResponse, head);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testDecodeExceptions() throws NoSuchMethodException {
        boolean error = false;
        try {
            jacksonSerializer.decode(null, null, null);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        try {
            jacksonSerializer.decode(null, Object[].class, null);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        Object o = null;
        try {
            jacksonSerializer.decode(null, o, null);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        o = String.class;
        try {
            jacksonSerializer.decode(null, o, null);
        } catch (SofaRpcException e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(EchoService.class.getName());
        request.setMethodName("checkOut");
        request.setMethod(EchoService.class.getMethod("checkOut", new Class[] { Echo.class, String.class }));
        Echo echo = new Echo();
        echo.setName("111");
        echo.setDel(true);
        request.setMethodArgs(new Object[] { echo, "test" });
        request.setMethodArgSigs(new String[] { Echo.class.getCanonicalName(), String.class.getCanonicalName() });
        request.setTargetServiceUniqueName(EchoService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 12);
        request.setTimeout(1024);
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        Map<String, String> map = new HashMap<String, String>(4);
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
