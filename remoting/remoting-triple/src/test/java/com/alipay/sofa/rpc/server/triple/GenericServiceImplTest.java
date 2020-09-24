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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.codec.sofahessian.SofaHessianSerializer;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.message.MessageBuilder;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.triple.TripleClientInvoker;
import io.grpc.Context;
import org.junit.Assert;
import org.junit.Test;
import triple.Request;
import triple.Response;

import java.lang.reflect.Method;

/**
 * @author zhaowang
 * @version : GenericServiceImplTest.java, v 0.1 2020年06月30日 11:10 上午 zhaowang Exp $
 */
public class GenericServiceImplTest {

    private static String                       serialization = "hessian2";
    private static Serializer                   serializer    = new SofaHessianSerializer();

    private GenericServiceImpl                  genericService;
    private MockStreamObserver<triple.Response> responseObserver;

    public GenericServiceImplTest(){
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(new HelloServiceImpl());
        providerConfig.setProxyClass(HelloService.class);
        genericService = new GenericServiceImpl(providerConfig.getRef(),providerConfig.getProxyClass());
        responseObserver = new MockStreamObserver<>();
    }

    @Test
    public void testPrimitiveType() throws NoSuchMethodException {
        // test return primitive type

        String methodName = "testPrimitiveType";
        Method method = HelloService.class.getDeclaredMethod(methodName, long.class);
        Object[] args = new Object[1];
        long param = 100L;
        args[0] = param;

        Request request = buildRequest(method, args);

        doInvoke(request);

        Object appResponse = getReturnValue(method);

        Assert.assertEquals(param, appResponse);

    }

    @Test
    public void testArray() throws Exception {
        // test return array

        String methodName = "testArray";
        Method method = HelloService.class.getDeclaredMethod(methodName, long[].class);
        Object[] args = new Object[1];
        long[] param = new long[1];
        long l = 100L;
        param[0] = l;
        args[0] = param;

        Request request = buildRequest(method, args);

        doInvoke(request);

        Object appResponse = getReturnValue(method);

        long[] appResponse1 = (long[]) appResponse;

        Assert.assertEquals(l, appResponse1[0]);

    }

    private void doInvoke(Request request) {
        genericService.generic(request, responseObserver);
    }

    private Object getReturnValue(Method method) {
        Object appResponse = null;
        Response response = responseObserver.getValue();
        byte[] responseDate = response.getData().toByteArray();
        Class returnType = method.getReturnType();
        if (returnType != void.class) {
            if (responseDate != null && responseDate.length > 0) {
                Serializer responseSerializer = SerializerFactory.getSerializer(response.getSerializeType());
                appResponse = responseSerializer.decode(new ByteArrayWrapperByteBuf(responseDate),
                    returnType,
                    null);
            }
        }
        return appResponse;
    }

    private Request buildRequest(Method method, Object[] args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        SofaRequest sofaRequest = MessageBuilder.buildSofaRequest(HelloService.class, method, parameterTypes, args);
        Request request = TripleClientInvoker.getRequest(sofaRequest, serialization, serializer);
        Context context = Context.current().withValue(TracingContextKey.getKeySofaRequest(), sofaRequest);
        context.attach();
        return request;
    }

}