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
package com.alipay.sofa.rpc.codec.sofahessian;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaHessianSerializerTest {

    private SofaHessianSerializer serializer = new SofaHessianSerializer();

    @Test
    public void getSerializerFactory() {
        Assert.assertEquals(SingleClassLoaderSofaSerializerFactory.class,
            serializer.getSerializerFactory(false, false).getClass());
        Assert.assertEquals(MultipleClassLoaderSofaSerializerFactory.class,
            serializer.getSerializerFactory(true, false).getClass());
        Assert.assertEquals(GenericSingleClassLoaderSofaSerializerFactory.class,
            serializer.getSerializerFactory(false, true).getClass());
        Assert.assertEquals(GenericMultipleClassLoaderSofaSerializerFactory.class,
            serializer.getSerializerFactory(true, true).getClass());
    }

    @Test
    public void encode() {
        AbstractByteBuf data = serializer.encode("xxx", null);
        String dst = (String) serializer.decode(data, String.class, null);
        Assert.assertEquals("xxx", dst);

        boolean error = false;
        try {
            serializer.decode(data, "", null);
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
            serializer.decode(data, (Object) null, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void encodeSofaRequest() throws Exception {
        SofaRequest request = buildRequest();
        AbstractByteBuf data = serializer.encode(request, null);
        SofaRequest newRequest = (SofaRequest) serializer.decode(data, SofaRequest.class, null);

        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertArrayEquals(newRequest.getMethodArgSigs(), request.getMethodArgSigs());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());

        request = buildRequest();
        GenericObject genericObject = new GenericObject(TestGenericBean.class.getCanonicalName());
        genericObject.putField("name", "Lilei");
        genericObject.putField("age", 123);
        request.setMethodArgSigs(new String[] { TestGenericBean.class.getCanonicalName() }); // change to generic request
        request.setMethodArgs(new Object[] { genericObject });
        data = serializer.encode(request, Collections.singletonMap(RemotingConstants.HEAD_GENERIC_TYPE,
            RemotingConstants.SERIALIZE_FACTORY_GENERIC));
        newRequest = (SofaRequest) serializer.decode(data, SofaRequest.class, null);
        Assert.assertEquals(newRequest.getInterfaceName(), request.getInterfaceName());
        Assert.assertEquals(newRequest.getMethodName(), request.getMethodName());
        Assert.assertArrayEquals(newRequest.getMethodArgSigs(), request.getMethodArgSigs());
        Assert.assertEquals(newRequest.getMethodArgs().length, request.getMethodArgs().length);
        Assert.assertEquals(TestGenericBean.class.getCanonicalName(), newRequest.getMethodArgs()[0].getClass()
            .getCanonicalName());
        Assert.assertEquals(newRequest.getTargetServiceUniqueName(), request.getTargetServiceUniqueName());
        Assert.assertEquals(newRequest.getTargetAppName(), request.getTargetAppName());

        // error request has no target service name
        request = buildRequest();
        request.setTargetServiceUniqueName(null);
        data = serializer.encode(request, null);
        boolean error = false;
        try {
            serializer.decode(data, SofaRequest.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void encodeSofaResponse() {
        AbstractByteBuf data;
        SofaResponse response = new SofaResponse();
        response.setErrorMsg("1233");
        Assert.assertTrue(response.isError());
        Assert.assertEquals("1233", response.getErrorMsg());
        data = serializer.encode(response, null);
        SofaResponse newResponse = (SofaResponse) serializer.decode(data, SofaResponse.class, null);
        Assert.assertTrue(newResponse.isError());
        SofaResponse newResponse2 = (SofaResponse) serializer.decode(data, SofaResponse.class,
            Collections.singletonMap(RemotingConstants.HEAD_GENERIC_TYPE,
                RemotingConstants.SERIALIZE_FACTORY_GENERIC));
        Assert.assertTrue(newResponse2.isError());

        response = new SofaResponse();
        response.setAppResponse("123");
        data = serializer.encode(response, null);
        newResponse = (SofaResponse) serializer.decode(data, SofaResponse.class, null);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
    }

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(Invoker.class.getName());
        request.setMethodName("invoke");
        request.setMethod(Invoker.class.getMethod("invoke", SofaRequest.class));
        request.setMethodArgs(new Object[] { new SofaRequest() });
        request.setMethodArgSigs(new String[] { SofaRequest.class.getCanonicalName() });
        request.setTargetServiceUniqueName(Invoker.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 11);
        request.setTimeout(1024);
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
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