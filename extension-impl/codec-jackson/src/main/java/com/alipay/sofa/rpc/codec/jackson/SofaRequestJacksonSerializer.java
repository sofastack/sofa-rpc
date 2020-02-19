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
package com.alipay.sofa.rpc.codec.jackson;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.hessian.generic.util.GenericUtils;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteStreamWrapperByteBuf;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class SofaRequestJacksonSerializer extends AbstractCustomJacksonSerializer<SofaRequest> {
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaRequest template)
            throws SofaRpcException {
        try {
            mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
            GenericObject genericObject = mapper.readValue(data.array(), GenericObject.class);
            Object tmp = GenericUtils.convertToObject(genericObject);
            String targetService = context.remove(RemotingConstants.HEAD_TARGET_SERVICE);
            if (targetService != null) {
                template.setTargetServiceUniqueName(targetService);
                String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetService);
                template.setInterfaceName(interfaceName);
            } else {
                throw buildDeserializeError("HEAD_TARGET_SERVICE is null");
            }

            if (tmp instanceof SofaRequest){
                template.setMethodArgSigs(((SofaRequest) tmp).getMethodArgSigs());
                template.setMethodArgs(((SofaRequest) tmp).getMethodArgs());
                template.setMethodName(((SofaRequest) tmp).getMethodName());
                template.addRequestProps(((SofaRequest) tmp).getRequestProps());
                template.setTargetAppName(((SofaRequest) tmp).getTargetAppName());
            }

        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage(), e);
        }

    }

    @Override
    public SofaRequest decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        try {
            UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
            Hessian2Input input = new Hessian2Input(inputStream);
            Object object = input.readObject();
            SofaRequest sofaRequest = (SofaRequest) object;
            String targetServiceName = sofaRequest.getTargetServiceUniqueName();
            if (targetServiceName == null) {
                throw buildDeserializeError("Target service name of request is null!");
            }
            String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetServiceName);
            sofaRequest.setInterfaceName(interfaceName);

            String[] sig = sofaRequest.getMethodArgSigs();
            Class<?>[] classSig = ClassTypeUtils.getClasses(sig);

            final Object[] args = new Object[sig.length];
            for (int i = 0; i < sofaRequest.getMethodArgSigs().length; ++i) {
                args[i] = input.readObject(classSig[i]);
            }
            sofaRequest.setMethodArgs(args);
            input.close();
            return sofaRequest;
        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage(), e);
        }
    }

    @Override
    public AbstractByteBuf encodeObject(SofaRequest sofaRequest, Map<String, String> context) {
        try {
            UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(outputStream);
            output.writeObject(sofaRequest);
            final Object[] args = sofaRequest.getMethodArgs();
            if (args != null) {
                for (Object arg : args) {
                    output.writeObject(arg);
                }
            }
            output.close();

            return new ByteStreamWrapperByteBuf(outputStream);
        } catch (IOException e) {
            throw buildSerializeError(e.getMessage(), e);
        }
    }
}