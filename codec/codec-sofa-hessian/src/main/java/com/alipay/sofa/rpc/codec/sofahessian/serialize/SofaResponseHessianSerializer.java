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

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteStreamWrapperByteBuf;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class SofaResponseHessianSerializer extends AbstractCustomHessianSerializer<SofaResponse> {

    public SofaResponseHessianSerializer(SerializerFactory serializerFactory,
                                         SerializerFactory genericSerializerFactory) {
        super(serializerFactory, genericSerializerFactory);
    }

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaResponse template)
        throws SofaRpcException {
        try {
            UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
            Hessian2Input input = new Hessian2Input(inputStream);
            // 根据SerializeType信息决定序列化器
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                input.setSerializerFactory(genericSerializerFactory);
                GenericObject genericObject = (GenericObject) input.readObject();
                template.setErrorMsg((String) genericObject.getField("errorMsg"));
                template.setAppResponse(genericObject.getField("appResponse"));
                template.setResponseProps((Map<String, String>) genericObject.getField("responseProps"));
            } else {
                input.setSerializerFactory(serializerFactory);
                SofaResponse tmp = (SofaResponse) input.readObject();
                // copy values to template
                template.setErrorMsg(tmp.getErrorMsg());
                template.setAppResponse(tmp.getAppResponse());
                template.setResponseProps(tmp.getResponseProps());
            }
            input.close();
        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage(), e);
        }
    }

    @Override
    public SofaResponse decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        try {
            UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
            Hessian2Input input = new Hessian2Input(inputStream);
            // 根据SerializeType信息决定序列化器
            Object object;
            boolean genericSerialize = context != null && isGenericResponse(
                context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                input.setSerializerFactory(genericSerializerFactory);
                GenericObject genericObject = (GenericObject) input.readObject();
                SofaResponse sofaResponse = new SofaResponse();
                sofaResponse.setErrorMsg((String) genericObject.getField("errorMsg"));
                sofaResponse.setAppResponse(genericObject.getField("appResponse"));
                sofaResponse.setResponseProps((Map<String, String>) genericObject.getField("responseProps"));
                object = sofaResponse;
            } else {
                input.setSerializerFactory(serializerFactory);
                object = input.readObject();
            }
            input.close();
            return (SofaResponse) object;
        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage(), e);
        }
    }

    @Override
    public AbstractByteBuf encodeObject(SofaResponse sofaResponse, Map<String, String> context) {
        try {
            UnsafeByteArrayOutputStream byteArray = new UnsafeByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(byteArray);
            output.setSerializerFactory(serializerFactory);
            output.writeObject(sofaResponse);
            output.close();
            return new ByteStreamWrapperByteBuf(byteArray);
        } catch (IOException e) {
            throw buildSerializeError(e.getMessage(), e);
        }
    }
}