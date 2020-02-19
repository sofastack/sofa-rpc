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

import com.alipay.hessian.generic.model.GenericArray;
import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.hessian.generic.util.GenericUtils;
import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.utils.CodecUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Json serializer.
 * <p>
 * Encode: : Support String, SofaRequest and SofaResponse.
 * <p>
 * Decode by class mode : Support String.
 * <p>
 * Decode by object template : Support SofaRequest and SofaResponse.
 * <ul>
 * <li>encodeRequest: No need extra context</li>
 * <li>encodeResponse: No need extra context</li>
 * <li>decodeRequest: Need extra context which contains: HEAD_TARGET_SERVICE, HEAD_METHOD_NAME, HEAD_TARGET_APP,
 * RPC_TRACE_NAME, etc. </li>
 * <li>decodeResponse: Need extra context which contains: HEAD_RESPONSE_ERROR, HEAD_TARGET_SERVICE,
 * HEAD_METHOD_NAME </li>
 * </ul>
 *
 * @author <a href=mailto:zhiyuan.lzy@antfin.com>zhiyuan.lzy</a>
 */
@Extension(value = "json", code = 12)
public class JacksonSerializer extends AbstractSerializer {

    private ObjectMapper mapper = new ObjectMapper();

    private JacksonHelper jacksonHelper = new JacksonHelper();

    public JacksonSerializer() {
        CustomJacksonSerializerManager.addSerializer(SofaRequest.class,
                new SofaRequestJacksonSerializer());
       /* CustomJacksonSerializerManager.addSerializer(SofaResponse.class,
                new SofaResponseJacksonSerializer(serializerFactory, genericSerializerFactory));*/
    }

    @Override
    public AbstractByteBuf encode(Object object, Map<String, String> context) throws SofaRpcException {
        GenericObject genericObject = GenericUtils.convertToGenericObject(object);


        if (object instanceof SofaRequest){
            GenericArray methodArgs = GenericUtils.convertToGenericObject(((SofaRequest) object).getMethodArgs());
            genericObject.putField("methodArgs",methodArgs);
        }

        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        } else {
            try {
                return new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(genericObject));
            } catch (JsonProcessingException e) {
                throw buildSerializeError(e.getMessage());
            }
        }
    }

    @Override
    public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {

        Object result = null;

        if (clazz == null) {
            throw buildDeserializeError("class is null!");
        } else if (data.readableBytes() <= 0) {
            try {
                result = clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return result;
        } else {
            try {
                result = mapper.readValue(data.array(), clazz);
            } catch (IOException e) {
                throw buildDeserializeError(e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void decode(AbstractByteBuf data, Object template, Map<String, String> context) throws SofaRpcException {

        if (template == null) {
            throw buildDeserializeError("template is null!");
        } else {
            CustomJacksonSerializer serializer = CustomJacksonSerializerManager.getSerializer(template.getClass());
            if (serializer != null) {
                serializer.decodeObjectByTemplate(data, context, template);
            } else {
                throw buildDeserializeError(template.getClass() + " template is not supported");
            }
        }
    }
}
