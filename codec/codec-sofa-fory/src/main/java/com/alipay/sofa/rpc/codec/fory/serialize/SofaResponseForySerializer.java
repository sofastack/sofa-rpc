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
package com.alipay.sofa.rpc.codec.fory.serialize;

import com.alipay.sofa.rpc.codec.CustomSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.memory.MemoryBuffer;

import java.util.Map;

/**
 * Custom serializer for {@link SofaResponse} using Apache Fory.
 *
 * @author <a href="mailto:sunhailin.shl@antgroup.com">sunhailin-Leo</a>
 */
public class SofaResponseForySerializer implements CustomSerializer<SofaResponse> {

    private final ThreadSafeFory fory;

    public SofaResponseForySerializer(ThreadSafeFory fory) {
        this.fory = fory;
    }

    @Override
    public AbstractByteBuf encodeObject(SofaResponse object, Map<String, String> context) throws SofaRpcException {
        try {
            MemoryBuffer writeBuffer = MemoryBuffer.newHeapBuffer(32);
            writeBuffer.writerIndex(0);
            fory.serialize(writeBuffer, object);
            return new ByteArrayWrapperByteBuf(writeBuffer.getBytes(0, writeBuffer.writerIndex()));
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            // Fixed: was SERVER_DESERIALIZE, should be SERVER_SERIALIZE for encoding path
            throw new SofaRpcException(RpcErrorType.SERVER_SERIALIZE, e.getMessage(), e);
        }
    }

    @Override
    public SofaResponse decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
        try {
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                // TODO support generic call
                // Fixed: was CLIENT_SERIALIZE, should be CLIENT_DESERIALIZE for decoding path
                throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, "Generic call is not supported for now.");
            }
            return (SofaResponse) fory.deserialize(readBuffer);
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            // Fixed: was CLIENT_SERIALIZE, should be CLIENT_DESERIALIZE for decoding path
            throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, e.getMessage(), e);
        }
    }

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaResponse template)
        throws SofaRpcException {
        if (data.readableBytes() <= 0) {
            // Fixed: was CLIENT_SERIALIZE, should be CLIENT_DESERIALIZE for decoding path
            throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, "Deserialized array is empty.");
        }
        try {
            MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                // TODO support generic call
                throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, "Generic call is not supported for now.");
            } else {
                SofaResponse tmp = (SofaResponse) fory.deserialize(readBuffer);
                template.setErrorMsg(tmp.getErrorMsg());
                template.setAppResponse(tmp.getAppResponse());
                template.setResponseProps(tmp.getResponseProps());
            }
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, e.getMessage(), e);
        }
    }

    protected boolean isGenericResponse(String serializeType) {
        return serializeType != null && serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_GENERIC);
    }
}
