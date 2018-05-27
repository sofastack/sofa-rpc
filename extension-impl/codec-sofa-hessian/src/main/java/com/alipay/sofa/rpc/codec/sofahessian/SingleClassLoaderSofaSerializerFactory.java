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

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.caucho.hessian.io.ArrayDeserializer;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.JavaSerializer;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;

/**
 * SofaSerializerFactory used in single class loader.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class SingleClassLoaderSofaSerializerFactory extends SerializerFactory {

    /** 
     * logger for this class 
     */
    private static final Logger LOGGER = LoggerFactory
                                           .getLogger(SingleClassLoaderSofaSerializerFactory.class);

    @Override
    protected Serializer getDefaultSerializer(Class cl) {
        if (_defaultSerializer != null) {
            return _defaultSerializer;
        }

        return new JavaSerializer(cl);
    }

    @Override
    public Deserializer getDeserializer(String type) throws HessianProtocolException {
        if (StringUtils.isEmpty(type)) {
            return null;
        }

        Deserializer deserializer = getDeserializerFromCachedType(type);

        if (deserializer != null) {
            return deserializer;
        }

        deserializer = (Deserializer) _staticTypeMap.get(type);
        if (deserializer != null) {
            return deserializer;
        }

        if (type.startsWith("[")) {
            Deserializer subDeserializer = getDeserializer(type.substring(1));
            deserializer = new ArrayDeserializer(subDeserializer);
        } else {
            try {
                ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
                Class<?> cl = Class.forName(type, true, appClassLoader);
                deserializer = getDeserializer(cl);
            } catch (Exception e) {
                if (e instanceof ClassNotFoundException) {
                    LOGGER.errorWithApp(null, LogCodes.getLog(LogCodes.ERROR_DECODE_CLASS_NOT_FOUND,
                        getClass().getName(), type, Thread.currentThread().getContextClassLoader()));
                } else {
                    LOGGER.errorWithApp(null, e.toString(), e);
                }
            }
        }

        if (deserializer != null) {
            putDeserializerToCachedType(type, deserializer);
        }

        return deserializer;
    }

    protected Deserializer getDeserializerFromCachedType(String type) {
        return (Deserializer) _cachedTypeDeserializerMap.get(type);
    }

    protected void putDeserializerToCachedType(String type, Deserializer deserializer) {
        _cachedTypeDeserializerMap.put(type, deserializer);
    }
}
