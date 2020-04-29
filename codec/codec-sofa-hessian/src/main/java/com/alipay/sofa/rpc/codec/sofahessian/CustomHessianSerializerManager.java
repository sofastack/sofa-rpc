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

import com.alipay.sofa.rpc.codec.sofahessian.serialize.CustomHessianSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class CustomHessianSerializerManager {

    private final static Map<Class, CustomHessianSerializer> CUSTOM_SERIALIZERS = new ConcurrentHashMap<Class, CustomHessianSerializer>(
                                                                                    2);

    public static CustomHessianSerializer getSerializer(Class clazz) {
        return CUSTOM_SERIALIZERS.get(clazz);
    }

    public static void addSerializer(Class clazz, CustomHessianSerializer serializerManager) {
        CUSTOM_SERIALIZERS.put(clazz, serializerManager);
    }
}
