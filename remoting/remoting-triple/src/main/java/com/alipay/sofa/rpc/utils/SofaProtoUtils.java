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

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static com.alipay.sofa.rpc.common.RpcConstants.*;

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
        switch (callType) {
            case INVOKER_TYPE_ONEWAY:
            case INVOKER_TYPE_FUTURE:
            case INVOKER_TYPE_CALLBACK:
            case INVOKER_TYPE_SYNC:
                return MethodDescriptor.MethodType.UNARY;
            case INVOKER_TYPE_BI_STREAMING:
                return MethodDescriptor.MethodType.BIDI_STREAMING;
            case INVOKER_TYPE_CLIENT_STREAMING:
                return MethodDescriptor.MethodType.CLIENT_STREAMING;
            case INVOKER_TYPE_SERVER_STREAMING:
                return MethodDescriptor.MethodType.SERVER_STREAMING;
            default:
                throw new SofaRpcException(RpcErrorType.CLIENT_CALL_TYPE, "Unsupported invoke type:" + callType);
        }
    }

}