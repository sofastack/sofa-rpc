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

import com.alipay.hessian.ClassNameResolver;
import com.alipay.hessian.NameBlackListFilter;
import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.codec.sofahessian.serialize.CustomHessianSerializer;
import com.alipay.sofa.rpc.codec.sofahessian.serialize.SofaRequestHessianSerializer;
import com.alipay.sofa.rpc.codec.sofahessian.serialize.SofaResponseHessianSerializer;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteStreamWrapperByteBuf;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Serializer of SOFAHessian
 * <<p>
 * Encode: Object, SofaRequest and SofaResponse.
 * <p>
 * Decode by class mode : Support SofaRequest and SofaResponse.
 * <p>
 * Decode by object template : Support SofaRequest and SofaResponse.
 * <ul>
 * <li>encodeRequest: Need extra context which contains: HEAD_GENERIC_TYPE.</li>
 * <li>encodeResponse: No need extra context.</li>
 * <li>decodeRequest: No need extra context.</li>
 * <li>decodeResponse: Need extra context which contains: HEAD_GENERIC_TYPE.</li>
 * </ul>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "hessian2", code = 1)
public class SofaHessianSerializer extends AbstractSerializer {

    /** Logger for SofaHessianSerializer **/
    private static final Logger LOGGER = LoggerFactory.getLogger(SofaHessianSerializer.class);
    /**
     * Normal Serializer Factory
     */
    protected SerializerFactory serializerFactory;
    /**
     * Generic Serializer Factory
     */
    protected SerializerFactory genericSerializerFactory;

    /**
     * Instantiates a new Sofa hessian serializer.
     */
    public SofaHessianSerializer() {
        try {
            ClassUtils.forName("com.caucho.hessian.io.ShortHandle");
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Version of sofa-hessian is v4.x");
            }
        } catch (Exception e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Version of sofa-hessian is v3.x");
            }
        }
        boolean enableMultipleClassLoader = RpcConfigs.getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE);
        serializerFactory = getSerializerFactory(enableMultipleClassLoader, false);
        genericSerializerFactory = getSerializerFactory(enableMultipleClassLoader, true);
        if (RpcConfigs.getBooleanValue(RpcOptions.SERIALIZE_BLACKLIST_ENABLE) &&
            SofaConfigs.getBooleanValue(SofaOptions.CONFIG_SERIALIZE_BLACKLIST, true)) {
            ClassNameResolver resolver = new ClassNameResolver();
            resolver.addFilter(new NameBlackListFilter(BlackListFileLoader.SOFA_SERIALIZE_BLACK_LIST, 8192));
            serializerFactory.setClassNameResolver(resolver);
            genericSerializerFactory.setClassNameResolver(resolver);
        } else {
            serializerFactory.setClassNameResolver(null);
            genericSerializerFactory.setClassNameResolver(null);
        }
        CustomHessianSerializerManager.addSerializer(SofaRequest.class,
            new SofaRequestHessianSerializer(serializerFactory, genericSerializerFactory));
        CustomHessianSerializerManager.addSerializer(SofaResponse.class,
            new SofaResponseHessianSerializer(serializerFactory, genericSerializerFactory));
    }

    /**
     * Gets serializer factory.
     *
     * @param multipleClassLoader the multiple class loader
     * @param generic             the generic
     * @return the serializer factory
     */
    protected SerializerFactory getSerializerFactory(boolean multipleClassLoader, boolean generic) {
        if (generic) {
            return multipleClassLoader ? new GenericMultipleClassLoaderSofaSerializerFactory() :
                new GenericSingleClassLoaderSofaSerializerFactory();
        } else {
            return multipleClassLoader ? new MultipleClassLoaderSofaSerializerFactory() :
                new SingleClassLoaderSofaSerializerFactory();
        }
    }

    @Override
    public AbstractByteBuf encode(Object object, Map<String, String> context) {

        CustomHessianSerializer serializer = getCustomSerializer(object);
        if (serializer != null) {
            return serializer.encodeObject(object, context);
        } else {
            UnsafeByteArrayOutputStream byteArray = new UnsafeByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(byteArray);
            try {
                output.setSerializerFactory(serializerFactory);
                output.writeObject(object);
                output.close();
                return new ByteStreamWrapperByteBuf(byteArray);
            } catch (Exception e) {
                throw buildSerializeError(e.getMessage(), e);
            }

        }
    }

    @Override
    public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {
        if (clazz == null) {
            throw buildDeserializeError("class is null!");
        } else {
            CustomHessianSerializer serializer = CustomHessianSerializerManager.getSerializer(clazz);
            if (serializer != null) {
                return serializer.decodeObject(data, context);
            } else {
                try {
                    UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
                    Hessian2Input input = new Hessian2Input(inputStream);
                    input.setSerializerFactory(serializerFactory);
                    Object object = input.readObject();
                    input.close();
                    return object;
                } catch (IOException e) {
                    throw buildDeserializeError(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void decode(AbstractByteBuf data, Object template, Map<String, String> context) throws SofaRpcException {
        if (template == null) {
            throw buildDeserializeError("template is null!");
        } else {
            CustomHessianSerializer serializer = getCustomSerializer(template);
            if (serializer != null) {
                serializer.decodeObjectByTemplate(data, context, template);
            } else {
                throw buildDeserializeError(template.getClass() + " template is not supported");
            }
        }
    }

    private static CustomHessianSerializer getCustomSerializer(Object obj) {
        if (obj == null) {
            return null;
        }
        return CustomHessianSerializerManager.getSerializer(obj.getClass());
    }
}
