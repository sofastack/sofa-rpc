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
package com.alipay.sofa.rpc.utils;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import com.google.protobuf.ByteString;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import triple.Request;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.common.RpcConstants.INVOKER_TYPE_BI_STREAMING;
import static com.alipay.sofa.rpc.common.RpcConstants.INVOKER_TYPE_CLIENT_STREAMING;
import static com.alipay.sofa.rpc.common.RpcConstants.INVOKER_TYPE_SERVER_STREAMING;

/**
 * @author zhaowang
 * @version : SofaProtoUtils.java, v 0.1 2020年05月27日 7:25 下午 zhaowang Exp $
 */
public class SofaProtoUtils {

    public static boolean isProtoClass(Object object) {
        return object instanceof BindableService;
    }

    public static Set<String> getMethodNames(String interfaceId) {
        HashSet<String> result = new HashSet<>();
        Class interfaceClass = ClassUtils.forName(interfaceId);
        Method[] methods = interfaceClass.getMethods();
        for (Method method : methods) {
            result.add(method.getName());
        }
        return result;
    }

    public static boolean checkIfUseGeneric(ConsumerConfig consumerConfig) {
        Class proxyClass = consumerConfig.getProxyClass();
        Class enclosingClass = proxyClass.getEnclosingClass();
        if (enclosingClass != null) {
            try {
                enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class, int.class);
                return false;
            } catch (NoSuchMethodException e) {
                //ignore
                return true;
            }
        }

        return true;
    }

    public static MethodDescriptor.MethodType mapGrpcCallType(String callType) {
        return Optional.ofNullable(callType).map(type -> {
            switch (type) {
                case INVOKER_TYPE_BI_STREAMING:
                    return MethodDescriptor.MethodType.BIDI_STREAMING;
                case INVOKER_TYPE_CLIENT_STREAMING:
                    return MethodDescriptor.MethodType.CLIENT_STREAMING;
                case INVOKER_TYPE_SERVER_STREAMING:
                    return MethodDescriptor.MethodType.SERVER_STREAMING;
                default:
                    throw new SofaRpcException(RpcErrorType.CLIENT_CALL_TYPE, "Unsupported invoke type:" + callType);
            }
        }).orElse(MethodDescriptor.MethodType.UNARY);
    }

    public static Map<String, String> cacheStreamCallType(Class proxyClass) {
        Map<String, String> methodCallType = new ConcurrentHashMap<>();
        Method[] declaredMethods = proxyClass.getMethods();
        for (Method method : declaredMethods) {
            String streamType = mapStreamType(method);
            if (StringUtils.isNotBlank(streamType)) {
                methodCallType.put(method.getName(), streamType);
            }
        }
        return methodCallType;
    }

    /**
     * Gets the stream call type of certain method
     *
     * @param method the method
     * @return call type,server/client/bidirectional stream or default value. If not mapped to any stream call type, use the default value
     */
    private static String mapStreamType(Method method) {
        Class<?>[] paramClasses = method.getParameterTypes();
        Class<?> returnClass = method.getReturnType();

        int paramLen = paramClasses.length;

        //BidirectionalStream & ClientStream
        if (paramLen > 0 && SofaStreamObserver.class.isAssignableFrom(paramClasses[0]) && SofaStreamObserver.class.isAssignableFrom(returnClass)) {
            if (paramLen > 1) {
                throw new SofaRpcException(RpcErrorType.CLIENT_CALL_TYPE, "Bidirectional/Client stream method parameters can be only one StreamHandler.");
            }
            return RpcConstants.INVOKER_TYPE_BI_STREAMING;
        }
        //ServerStream
        else if (paramLen > 1 && SofaStreamObserver.class.isAssignableFrom(paramClasses[paramLen -1]) && void.class == returnClass) {
            return RpcConstants.INVOKER_TYPE_SERVER_STREAMING;
        } else if (SofaStreamObserver.class.isAssignableFrom(returnClass) || Arrays.stream(paramClasses).anyMatch(SofaStreamObserver.class::isAssignableFrom)) {
            throw new SofaRpcException(RpcErrorType.CLIENT_CALL_TYPE, "SofaStreamObserver can only at the specified location of parameter.Please check related docs.");
        }
        return null;
    }

    public static Request buildRequest(String[] methodArgSigs, Object[] methodArgs, String serialization,
                                       Serializer serializer, int backOffset) {
        Request.Builder builder = Request.newBuilder();
        builder.setSerializeType(serialization);
        for (int i = 0; i < methodArgSigs.length - backOffset; i++) {
            Object arg = methodArgs[i];
            ByteString argByteString = ByteString.copyFrom(serializer.encode(arg, null).array());
            builder.addArgs(argByteString);
            builder.addArgTypes(methodArgSigs[i]);
        }
        return builder.build();
    }
}