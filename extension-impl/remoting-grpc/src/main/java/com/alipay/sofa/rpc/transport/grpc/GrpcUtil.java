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
package com.alipay.sofa.rpc.transport.grpc;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.09 4:55 PM
 */
public class GrpcUtil {

    private final static ConcurrentHashMap<String, ManagedChannel> CACHE_ADDRESS_CHANNEL         = new ConcurrentHashMap<String, ManagedChannel>();

    private final static ConcurrentHashMap<String, Method>         CACHE_SERVICE_NEW_STUB_METHOD = new ConcurrentHashMap<String, Method>();

    private final static String                                    ADDRESS_KEY_SEPARATE          = "#";

    private final static String                                    INNER_CLASS_SEPARATE          = "$";

    private final static String                                    NEW_STUB_METHOD_NAME          = "newStub";


    public static Object buildStub(String serviceName, ManagedChannel channel){
        Method method = CACHE_SERVICE_NEW_STUB_METHOD.get(serviceName);
        if(method == null){
            method = getNewStubMethod(serviceName);
            CACHE_SERVICE_NEW_STUB_METHOD.put(serviceName, method);
        }

        try {
            return method.invoke(null, channel);
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e);
        }
    }

    public static Object getStub(String serviceName, String host, int port) {
        String channelKey = buildChannelKey(host, port);
        ManagedChannel channel = CACHE_ADDRESS_CHANNEL.get(channelKey);
        if (channel == null) {
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            CACHE_ADDRESS_CHANNEL.put(channelKey, channel);
        }

        Method method = CACHE_SERVICE_NEW_STUB_METHOD.get(serviceName);
        if (method == null) {
            method = getNewStubMethod(serviceName);
            CACHE_SERVICE_NEW_STUB_METHOD.put(serviceName, method);
        }

        try {
            return method.invoke(null, channel);
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e);
        }
    }

    public static ManagedChannel getChannel(String host, int port) {
        ManagedChannel channel = CACHE_ADDRESS_CHANNEL.get(buildChannelKey(host, port));

        return channel;
    }

    private static Method getNewStubMethod(String serviceName) {
        String grpcClassName = serviceName.substring(0, serviceName.indexOf(INNER_CLASS_SEPARATE));
        Method[] methods = ClassUtils.forName(grpcClassName).getMethods();
        for (Method method : methods) {
            if (method.getName().equals(NEW_STUB_METHOD_NAME)) {
                return method;
            }
        }

        return null;
    }

    private static String buildChannelKey(String host, int port) {
        return host + ADDRESS_KEY_SEPARATE + port;
    }
}