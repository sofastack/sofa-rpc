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
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Util for GRPC client transport
 *
 * @author LiangEn.LiWei
 * @date 2018.11.09 4:55 PM
 */
public class GrpcClientTransportUtil {

    private final static ConcurrentHashMap<String, HashMap<String, MethodDescriptor>> CACHE_SERVICE_METHOD_DESCRIPTOR  = new ConcurrentHashMap<String,
                                                                                                                               HashMap<String, MethodDescriptor>>();

    private final static String                                                       INNER_CLASS_SEPARATE             = "$";

    private final static String                                                       FULL_METHOD_NAME_SEPARATOR       = "/";

    private final static String                                                       GRPC_GET_SERVICE_DESCRIPTOR_NAME = "getServiceDescriptor";

    public static Map<String, MethodDescriptor> getMethodDescriptors(String serviceName) {
        HashMap<String, MethodDescriptor> methodDescriptors = CACHE_SERVICE_METHOD_DESCRIPTOR.get(serviceName);
        if (methodDescriptors == null) {
            try {
                String grpcClassName = getGrpcClassName(serviceName);
                ServiceDescriptor serviceDescriptor = (ServiceDescriptor) ClassUtils.forName(grpcClassName).getMethod(
                    GRPC_GET_SERVICE_DESCRIPTOR_NAME, null).invoke(null, null);
                Collection<MethodDescriptor<?, ?>> source = serviceDescriptor.getMethods();
                if (!source.isEmpty()) {
                    methodDescriptors = new HashMap<String, MethodDescriptor>();
                    for (MethodDescriptor methodDescriptor : source) {
                        methodDescriptors.put(interceptMethodName(methodDescriptor.getFullMethodName()),
                            methodDescriptor);
                    }
                    CACHE_SERVICE_METHOD_DESCRIPTOR.put(serviceName, methodDescriptors);
                }
            } catch (Exception e) {
                throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e);
            }
        }

        return methodDescriptors;
    }

    private static String interceptMethodName(String fullMethodName) {
        StringBuffer ret = new StringBuffer();
        String name = null;
        if (fullMethodName != null && fullMethodName.length() > 0) {
            name = fullMethodName.substring(fullMethodName.lastIndexOf(FULL_METHOD_NAME_SEPARATOR) + 1);
            ret.append(String.valueOf((name.charAt(0))).toLowerCase());
            if (fullMethodName.length() > 1) {
                ret.append(name.substring(1));
            }
        }
        return ret.toString();
    }

    private static String getGrpcClassName(String serviceName) {
        int idx = serviceName.indexOf(INNER_CLASS_SEPARATE);
        return idx == -1 ? serviceName : serviceName.substring(0, serviceName.indexOf(INNER_CLASS_SEPARATE));
    }
}