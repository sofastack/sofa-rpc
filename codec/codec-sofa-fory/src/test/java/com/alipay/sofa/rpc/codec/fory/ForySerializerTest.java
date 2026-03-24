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
package com.alipay.sofa.rpc.codec.fory;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.fory.model.blacklist.BlackListClass;
import com.alipay.sofa.rpc.codec.fory.model.none.NoneClassHasBlackClass;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoRequest;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoResponse;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoService;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.WhiteClassHasBlackClass;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link ForySerializer}.
 *
 * @author <a href="mailto:sunhailin.shl@antgroup.com">sunhailin-Leo</a>
 */
public class ForySerializerTest {

    private final ForySerializer serializer = (ForySerializer) ExtensionLoaderFactory.getExtensionLoader(
                                                Serializer.class).getExtension("fory");

    @Test
    public void encodeAndDecode() {
        try {
            serializer.encode(null, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("a");
        AbstractByteBuf byteBuf = serializer.encode(demoRequest, null);
        DemoRequest req2 = (DemoRequest) serializer.decode(byteBuf, DemoRequest.class, null);
        Assert.assertEquals(demoRequest.getName(), req2.getName());

        AbstractByteBuf data = serializer.encode("xxx", null);
        String dst = (String) serializer.decode(data, String.class, null);
        Assert.assertEquals("xxx", dst);

        try {
            serializer.decode(data, null, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            serializer.decode(data, "", null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSofaRequest() throws Exception {
        SofaRequest request = buildRequest();
        AbstractByteBuf data = serializer.encode(request, null);

        serializer.encode("123456", null);
        SofaRequest decode = (SofaRequest) serializer.decode(data, SofaRequest.class, null);
        assertEqualsSofaRequest(request, decode);

        SofaRequest newRequest = new SofaRequest();
        serializer.decode(data, newRequest, null);
        assertEqualsSofaRequest(request, newRequest);

        // null request
        newRequest = new SofaRequest();
        try {
            serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSofaResponse() throws Exception {
        SofaResponse response = new SofaResponse();
        final DemoResponse demoAppResponse = new DemoResponse();
        demoAppResponse.setWord("result");
        response.setAppResponse(demoAppResponse);
        AbstractByteBuf data = serializer.encode(response, null);

        try {
            serializer.decode(data, null, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            serializer.decode(data, new Object(), null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        SofaResponse decode = (SofaResponse) serializer.decode(data, SofaResponse.class, null);
        Assert.assertFalse(decode.isError());
        Assert.assertEquals(response.getAppResponse(), decode.getAppResponse());
        Assert.assertEquals("result", ((DemoResponse) decode.getAppResponse()).getWord());

        // success response via template
        SofaResponse newResponse = new SofaResponse();
        serializer.decode(data, newResponse, null);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
        Assert.assertEquals("result", ((DemoResponse) newResponse.getAppResponse()).getWord());

        // null response
        newResponse = new SofaResponse();
        try {
            serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newResponse, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        // error response
        response = new SofaResponse();
        response.setErrorMsg("1233");
        data = serializer.encode(response, null);
        newResponse = new SofaResponse();
        serializer.decode(data, newResponse, null);
        Assert.assertTrue(newResponse.isError());
        Assert.assertEquals(response.getErrorMsg(), newResponse.getErrorMsg());
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
        request.setSerializeType((byte) 23);
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

    private void assertEqualsSofaRequest(SofaRequest request, SofaRequest decode) {
        // Fixed: assertEquals(expected, actual) — expected first
        Assert.assertEquals(request.getInterfaceName(), decode.getInterfaceName());
        Assert.assertEquals(request.getMethodName(), decode.getMethodName());
        Assert.assertArrayEquals(request.getMethodArgSigs(), decode.getMethodArgSigs());
        Assert.assertEquals(request.getMethodArgs().length, decode.getMethodArgs().length);
        Assert.assertEquals("name", ((DemoRequest) decode.getMethodArgs()[0]).getName());
        Assert.assertEquals(request.getTargetServiceUniqueName(), decode.getTargetServiceUniqueName());
        Assert.assertEquals(request.getTargetAppName(), decode.getTargetAppName());
        Assert.assertEquals(request.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            decode.getRequestProp(RemotingConstants.RPC_TRACE_NAME));
    }

    @Test
    public void testChecker() throws Exception {
        // default fory checkMode is STRICT
        WhiteClassHasBlackClass whiteClassNullBlackClass = new WhiteClassHasBlackClass();
        NoneClassHasBlackClass noneClassNullBlackClass = new NoneClassHasBlackClass();

        BlackListClass blackListClass = new BlackListClass();
        WhiteClassHasBlackClass whiteClassHasBlackClass = new WhiteClassHasBlackClass();
        whiteClassHasBlackClass.setBlackListClass(blackListClass);
        NoneClassHasBlackClass noneClassHasBlackClass = new NoneClassHasBlackClass();
        noneClassHasBlackClass.setBlackListClass(blackListClass);

        try {
            serializer.encode(noneClassNullBlackClass, null);
            Assert.fail();
        } catch (Exception e) {
            // expected: NoneClass not in whitelist
        }

        try {
            serializer.encode(noneClassHasBlackClass, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            serializer.encode(blackListClass, null);
            Assert.fail();
        } catch (Exception e) {
            // expected: BlackListClass not in whitelist
        }

        serializer.encode(whiteClassNullBlackClass, null);
        try {
            serializer.encode(whiteClassHasBlackClass, null);
            Assert.fail();
        } catch (Exception e) {
            // expected: contains blacklist class
        }

        // test WARN mode — wrapped in try-finally to guarantee system property cleanup
        System.getProperties().put("sofa.rpc.codec.serialize.checkMode", "WARN");
        try {
            ForySerializer warnForySerializer = new ForySerializer();

            warnForySerializer.encode(noneClassNullBlackClass, null);

            try {
                warnForySerializer.encode(noneClassHasBlackClass, null);
                Assert.fail();
            } catch (Exception e) {
                // expected
            }

            try {
                warnForySerializer.encode(blackListClass, null);
                Assert.fail();
            } catch (Exception e) {
                // expected
            }

            warnForySerializer.encode(whiteClassNullBlackClass, null);
            try {
                warnForySerializer.encode(whiteClassHasBlackClass, null);
                Assert.fail();
            } catch (Exception e) {
                // expected
            }
        } finally {
            System.getProperties().remove("sofa.rpc.codec.serialize.checkMode");
        }

        // test DISABLE mode — wrapped in try-finally to guarantee system property cleanup
        System.getProperties().put("sofa.rpc.codec.serialize.checkMode", "DISABLE");
        try {
            ForySerializer disableForySerializer = new ForySerializer();
            disableForySerializer.encode(noneClassNullBlackClass, null);
            disableForySerializer.encode(noneClassHasBlackClass, null);
            disableForySerializer.encode(blackListClass, null);
            disableForySerializer.encode(whiteClassNullBlackClass, null);
            disableForySerializer.encode(whiteClassHasBlackClass, null);
        } finally {
            System.getProperties().remove("sofa.rpc.codec.serialize.checkMode");
        }
    }
}
