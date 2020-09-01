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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

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

    public static String getFullNameWithUniqueId(String fullMethodName, String uniqueId) {
        int i = fullMethodName.indexOf("/");
        if (i > 0 && StringUtils.isNotBlank(uniqueId)) {
            String[] split = fullMethodName.split("/");
            fullMethodName = split[0] + "." + uniqueId + "/" + split[1];
        }
        return fullMethodName;
    }

}