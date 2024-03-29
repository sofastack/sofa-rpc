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
package com.alipay.sofa.rpc.codec;

import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public abstract class AbstractSerializer implements Serializer {

    protected static Map<String, String> genericServiceMap = new ConcurrentHashMap<>();

    protected   Map<Class, CustomSerializer> customSerializers = new ConcurrentHashMap<>();

    protected SofaRpcException buildSerializeError(String message) {
        return new SofaRpcException(getErrorCode(true), LogCodes.getLog(LogCodes.ERROR_SERIALIZER, message));
    }

    protected SofaRpcException buildSerializeError(String message, Throwable throwable) {
        return new SofaRpcException(getErrorCode(true), LogCodes.getLog(LogCodes.ERROR_SERIALIZER, message), throwable);
    }

    protected SofaRpcException buildDeserializeError(String message) {
        return new SofaRpcException(getErrorCode(false), LogCodes.getLog(LogCodes.ERROR_SERIALIZER, message));
    }

    protected SofaRpcException buildDeserializeError(String message, Throwable throwable) {
        return new SofaRpcException(getErrorCode(false), LogCodes.getLog(LogCodes.ERROR_SERIALIZER, message), throwable);
    }

    /**
     * @param serialize true is serialize, false is deserialize.
     */
    private int getErrorCode(boolean serialize) {
        if (RpcInternalContext.getContext().isProviderSide()) {
            return serialize ? RpcErrorType.SERVER_SERIALIZE : RpcErrorType.SERVER_DESERIALIZE;
        } else if (RpcInternalContext.getContext().isConsumerSide()) {
            return serialize ? RpcErrorType.CLIENT_SERIALIZE : RpcErrorType.CLIENT_DESERIALIZE;
        } else {
            return RpcErrorType.UNKNOWN;
        }
    }

    //注册泛化接口对应的实际实现类型
    public static void registerGenericService(String serviceName, String className) {
        if (StringUtils.isNotBlank(serviceName) && StringUtils.isNotBlank(className)) {
            genericServiceMap.put(serviceName, className);
        }
    }

    @VisibleForTesting
    public static void clear() {
        genericServiceMap.clear();
    }

    protected CustomSerializer getObjCustomSerializer(Object obj) {
        if (obj == null) {
            return null;
        }
        return getCustomSerializer(obj.getClass());
    }

    protected CustomSerializer getCustomSerializer(Class clazz) {
        return customSerializers.get(clazz);
    }

    public void addCustomSerializer(Class clazz, CustomSerializer serializerManager) {
        customSerializers.put(clazz, serializerManager);
    }
}
