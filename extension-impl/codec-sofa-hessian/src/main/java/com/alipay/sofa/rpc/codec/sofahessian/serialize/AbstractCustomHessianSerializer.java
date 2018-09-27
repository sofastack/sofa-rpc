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
package com.alipay.sofa.rpc.codec.sofahessian.serialize;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.caucho.hessian.io.SerializerFactory;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public abstract class AbstractCustomHessianSerializer<T> implements CustomHessianSerializer<T> {

    /**
     * Normal Serializer Factory
     */
    protected SerializerFactory serializerFactory;
    /**
     * Generic Serializer Factory
     */
    protected SerializerFactory genericSerializerFactory;

    public AbstractCustomHessianSerializer(SerializerFactory serializerFactory,
                                           SerializerFactory genericSerializerFactory) {
        this.serializerFactory = serializerFactory;
        this.genericSerializerFactory = genericSerializerFactory;
    }

    protected SofaRpcException buildDeserializeError(String message) {
        return new SofaRpcException(getErrorCode(false), message);
    }

    protected SofaRpcException buildSerializeError(String message, Throwable throwable) {
        return new SofaRpcException(getErrorCode(true), message, throwable);
    }

    protected SofaRpcException buildDeserializeError(String message, Throwable throwable) {
        return new SofaRpcException(getErrorCode(false), message, throwable);
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

    /**
     * Is generic request boolean.
     *
     * @param serializeType the serialize type
     * @return the boolean
     */
    protected boolean isGenericRequest(String serializeType) {
        return serializeType != null && !serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_NORMAL);
    }

    /**
     * Is generic response boolean.
     *
     * @param serializeType the serialize type
     * @return the boolean
     */
    protected boolean isGenericResponse(String serializeType) {
        return serializeType != null && serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_GENERIC);
    }
}