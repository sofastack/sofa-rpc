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

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Util for GRPC client transport
 *
 * @author LiangEn.LiWei
 * @date 2018.11.09 4:55 PM
 */
public class GrpcClientTransportUtil {

    private final static ConcurrentHashMap<String, Method> CACHE_SERVICE_NEW_STUB_METHOD = new ConcurrentHashMap<String, Method>();

    private final static String                            INNER_CLASS_SEPARATE          = "$";

    private final static String                            NEW_STUB_METHOD_NAME          = "newStub";

    /**
     * Create GRPC stub
     *
     * @param serviceName service name
     * @param channel the ManagedChannel
     * @return GRPC stub
     */
    public static Object buildStub(String serviceName, ManagedChannel channel) {
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

    /**
     * Remove build method for creating GRPC stub
     *
     * @param serviceName service name
     */
    public static void removeStubMethod(String serviceName) {
        CACHE_SERVICE_NEW_STUB_METHOD.remove(serviceName);
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
}