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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implements, uses interface id as group key, method name as command key
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class DefaultSetterFactory implements SetterFactory {

    private static final Map<Method, HystrixCommand.Setter> SETTER_CACHE = new ConcurrentHashMap<Method, HystrixCommand.Setter>();

    @Override
    public HystrixCommand.Setter createSetter(FilterInvoker invoker, SofaRequest request) {
        Method clientMethod = request.getMethod();
        if (!SETTER_CACHE.containsKey(clientMethod)) {
            synchronized (DefaultSetterFactory.class) {
                if (!SETTER_CACHE.containsKey(clientMethod)) {
                    String interfaceId = invoker.getConfig().getInterfaceId();
                    String commandKey = generateCommandKey(interfaceId, request.getMethod());
                    HystrixCommand.Setter setter = HystrixCommand.Setter
                        .withGroupKey(HystrixCommandGroupKey.Factory.asKey(interfaceId))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
                    SETTER_CACHE.put(clientMethod, setter);
                }
            }
        }
        return SETTER_CACHE.get(clientMethod);
    }

    public static String generateCommandKey(String interfaceId, Method method) {
        StringBuilder builder = new StringBuilder(interfaceId)
            .append("#")
            .append(method.getName())
            .append("(");
        if (method.getParameterTypes().length > 0) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                builder.append(parameterType.getSimpleName()).append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.append(")").toString();
    }
}
