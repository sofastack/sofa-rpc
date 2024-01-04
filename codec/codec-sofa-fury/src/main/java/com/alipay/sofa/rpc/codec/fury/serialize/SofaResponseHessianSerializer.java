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
package com.alipay.sofa.rpc.codec.fury.serialize;

import com.alipay.sofa.rpc.codec.CustomSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import io.fury.ThreadLocalFury;
import io.fury.memory.MemoryBuffer;

import java.util.Map;

/**
 * @author Even
 * @date 2024/1/4 19:30
 */
public class SofaResponseHessianSerializer implements CustomSerializer<SofaResponse> {

    private final ThreadLocalFury fury;

    public SofaResponseHessianSerializer(ThreadLocalFury fury) {
        this.fury = fury;
    }

    @Override
    public AbstractByteBuf encodeObject(SofaResponse object, Map<String, String> context) throws SofaRpcException {
        try {
            fury.setClassLoader(Thread.currentThread().getContextClassLoader());
            MemoryBuffer writeBuffer = MemoryBuffer.newHeapBuffer(32);
            writeBuffer.writerIndex(0);
            fury.serialize(writeBuffer, object);
            return new ByteArrayWrapperByteBuf(writeBuffer.getBytes(0, writeBuffer.writerIndex()));
        } catch (Exception e) {
            throw new SofaRpcException(e.getMessage(), e);
        }
    }

    @Override
    public SofaResponse decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
        try {
            fury.setClassLoader(Thread.currentThread().getContextClassLoader());
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                // TODO support generic call
                throw new SofaRpcException("Generic call is not supported for now.");
            }
            return (SofaResponse) fury.deserialize(readBuffer);
        } catch (Exception e) {
            throw new SofaRpcException(e.getMessage(), e);
        }
    }

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaResponse template)
        throws SofaRpcException {
        if (data.readableBytes() <= 0) {
            throw new SofaRpcException("Deserialized array is empty.");
        }
        try {
            fury.setClassLoader(Thread.currentThread().getContextClassLoader());
            MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
            // 根据SerializeType信息决定序列化器
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                // TODO support generic call
                throw new SofaRpcException("Generic call is not supported for now.");
            } else {
                SofaResponse tmp = (SofaResponse) fury.deserialize(readBuffer);
                // copy values to template
                template.setErrorMsg(tmp.getErrorMsg());
                template.setAppResponse(tmp.getAppResponse());
                template.setResponseProps(tmp.getResponseProps());
            }
        } catch (Exception e) {
            throw new SofaRpcException(e.getMessage(), e);
        }
    }

    protected boolean isGenericResponse(String serializeType) {
        return serializeType != null && serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_GENERIC);
    }
}
