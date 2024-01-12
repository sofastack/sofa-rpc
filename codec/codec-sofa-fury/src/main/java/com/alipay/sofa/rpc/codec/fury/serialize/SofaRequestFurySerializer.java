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
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import io.fury.ThreadSafeFury;
import io.fury.memory.MemoryBuffer;

import java.util.Map;

/**
 * @author Even
 * @date 2024/1/4 19:29
 */
public class SofaRequestFurySerializer implements CustomSerializer<SofaRequest> {

    private final ThreadSafeFury fury;

    public SofaRequestFurySerializer(ThreadSafeFury fury) {
        this.fury = fury;
    }

    @Override
    public AbstractByteBuf encodeObject(SofaRequest object, Map<String, String> context) throws SofaRpcException {
        try {
            MemoryBuffer writeBuffer = MemoryBuffer.newHeapBuffer(32);
            writeBuffer.writerIndex(0);

            // 根据SerializeType信息决定序列化器
            boolean genericSerialize = context != null &&
                isGenericRequest(context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                // TODO support generic call
                throw new SofaRpcException(RpcErrorType.CLIENT_SERIALIZE, "Generic call is not supported for now.");
            }
            fury.serialize(writeBuffer, object);
            final Object[] args = object.getMethodArgs();
            fury.serialize(writeBuffer, args);

            return new ByteArrayWrapperByteBuf(writeBuffer.getBytes(0, writeBuffer.writerIndex()));
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_SERIALIZE, e.getMessage(), e);
        }
    }

    @Override
    public SofaRequest decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
        try {
            SofaRequest sofaRequest = (SofaRequest) fury.deserialize(readBuffer);
            String targetServiceName = sofaRequest.getTargetServiceUniqueName();
            if (targetServiceName == null) {
                throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Target service name of request is null!");
            }
            String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetServiceName);
            sofaRequest.setInterfaceName(interfaceName);
            final Object[] args = (Object[]) fury.deserialize(readBuffer);
            sofaRequest.setMethodArgs(args);
            return sofaRequest;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, e.getMessage(), e);
        }
    }

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaRequest template)
        throws SofaRpcException {
        if (data.readableBytes() <= 0) {
            throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Deserialized array is empty.");
        }
        try {
            MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
            SofaRequest tmp = (SofaRequest) fury.deserialize(readBuffer);
            String targetServiceName = tmp.getTargetServiceUniqueName();
            if (targetServiceName == null) {
                throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Target service name of request is null!");
            }
            // copy values to template
            template.setMethodName(tmp.getMethodName());
            template.setMethodArgSigs(tmp.getMethodArgSigs());
            template.setTargetServiceUniqueName(tmp.getTargetServiceUniqueName());
            template.setTargetAppName(tmp.getTargetAppName());
            template.addRequestProps(tmp.getRequestProps());
            String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetServiceName);
            template.setInterfaceName(interfaceName);
            final Object[] args = (Object[]) fury.deserialize(readBuffer);
            template.setMethodArgs(args);
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, e.getMessage(), e);
        }
    }

    protected boolean isGenericRequest(String serializeType) {
        return serializeType != null && !serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_NORMAL);
    }

}
