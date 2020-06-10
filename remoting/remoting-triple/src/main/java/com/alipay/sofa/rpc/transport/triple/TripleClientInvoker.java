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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.google.protobuf.ByteString;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import triple.Request;
import triple.Response;

import java.lang.reflect.Method;
import java.util.ResourceBundle;

import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_HESSIAN2;
import static com.alipay.sofa.rpc.utils.SofaProtoUtils.checkIfUseGeneric;
import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * Invoker for Grpc
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.12.15 7:06 PM
 */
public class TripleClientInvoker implements TripleInvoker {
    private final static Logger LOGGER                = LoggerFactory.getLogger(TripleClientInvoker.class);

    private final static String DEFAULT_SERIALIZATION = SERIALIZE_HESSIAN2;

    protected Channel           channel;

    protected ConsumerConfig    consumerConfig;

    protected Method            sofaStub;

    protected boolean           useGeneric;

    private Serializer          serializer;
    private String              serialization;

    public TripleClientInvoker(ConsumerConfig consumerConfig, Channel channel) {
        this.channel = channel;
        this.consumerConfig = consumerConfig;
        useGeneric = checkIfUseGeneric(consumerConfig);
        cacheCommonData(consumerConfig);

        if (!useGeneric) {
            Class enclosingClass = consumerConfig.getProxyClass().getEnclosingClass();
            try {
                sofaStub = enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class,
                    ProviderInfo.class, ConsumerConfig.class, int.class);
            } catch (NoSuchMethodException e) {
                LOGGER.error("getSofaStub not found in enclosingClass" + enclosingClass.getName());
            }
        }
    }

    private void cacheCommonData(ConsumerConfig consumerConfig) {
        String serialization = consumerConfig.getSerialization();
        if (StringUtils.isBlank(serialization)) {
            serialization = getDefaultSerialization();
        }
        this.serialization = serialization;
        this.serializer = SerializerFactory.getSerializer(serialization);
    }

    protected String getDefaultSerialization() {
        return DEFAULT_SERIALIZATION;
    }

    @Override
    public SofaResponse invoke(SofaRequest sofaRequest, int timeout)
        throws Exception {
        if (!useGeneric) {
            SofaResponse sofaResponse = new SofaResponse();
            ProviderInfo providerInfo = null;
            Object stub = sofaStub.invoke(null, channel, buildCustomCallOptions(sofaRequest, timeout), providerInfo,
                consumerConfig, timeout);
            final Method method = sofaRequest.getMethod();
            Object appResponse = method.invoke(stub, sofaRequest.getMethodArgs()[0]);
            sofaResponse.setAppResponse(appResponse);
            return sofaResponse;
        } else {
            String serviceName = sofaRequest.getInterfaceName();
            String methodName = sofaRequest.getMethodName();
            MethodDescriptor.Marshaller<?> requestMarshaller = null;
            MethodDescriptor.Marshaller<?> responseMarshaller = null;
            requestMarshaller = io.grpc.protobuf.ProtoUtils.marshaller(Request.getDefaultInstance());
            responseMarshaller = io.grpc.protobuf.ProtoUtils.marshaller(Response.getDefaultInstance());
            MethodDescriptor methodDescriptor = io.grpc.MethodDescriptor.newBuilder()
                .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(generateFullMethodName(serviceName, methodName)).setSampledToLocalTracing(true)
                .setRequestMarshaller((MethodDescriptor.Marshaller<Object>) requestMarshaller)
                .setResponseMarshaller((MethodDescriptor.Marshaller<Object>) responseMarshaller)
                .build();

            Request.Builder builder = Request.newBuilder();
            builder.setSerializeType(serialization);

            String[] methodArgSigs = sofaRequest.getMethodArgSigs();
            Object[] methodArgs = sofaRequest.getMethodArgs();

            for (int i = 0; i < methodArgSigs.length; i++) {
                builder.addArgs(ByteString.copyFrom(serializer.encode(methodArgs[i], null).array()));
                builder.addArgTypes(methodArgSigs[i]);
            }
            Request request = builder.build();

            Response response = (Response) ClientCalls.blockingUnaryCall(channel, methodDescriptor,
                buildCustomCallOptions(sofaRequest, timeout), request);

            byte[] responseDate = response.getData().toByteArray();
            Class returnType = ClassUtils.forName(response.getType());
            Object appResponse = serializer.decode(new ByteArrayWrapperByteBuf(responseDate), returnType, null);

            SofaResponse sofaResponse = new SofaResponse();
            sofaResponse.setAppResponse(appResponse);
            return sofaResponse;
        }

    }

    /**
     * set some custom info
     * @param sofaRequest
     * @param timeout
     * @return
     */
    protected CallOptions buildCustomCallOptions(SofaRequest sofaRequest, int timeout) {
        CallOptions tripleCallOptions = CallOptions.DEFAULT;
        final String target = consumerConfig.getParameter("interworking.target");
        if (StringUtils.isNotBlank(target)) {
            tripleCallOptions = tripleCallOptions.withAuthority(target);
        }
        return tripleCallOptions;
    }
}