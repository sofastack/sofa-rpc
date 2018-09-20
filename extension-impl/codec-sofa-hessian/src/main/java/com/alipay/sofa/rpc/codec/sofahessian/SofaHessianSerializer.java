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

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
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
        boolean enableMultipleClassLoader = RpcConfigs.getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE);
        serializerFactory = getSerializerFactory(enableMultipleClassLoader, false);
        genericSerializerFactory = getSerializerFactory(enableMultipleClassLoader, true);
        if (!RpcConfigs.getBooleanValue(RpcOptions.SERIALIZE_BLACKLIST_ENABLE) ||
            !SofaConfigs.getBooleanValue(SofaOptions.CONFIG_SERIALIZE_BLACKLIST, true)) {
            serializerFactory.setClassNameResolver(null);
            genericSerializerFactory.setClassNameResolver(null);
        }
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
        if (object instanceof SofaRequest) {
            return encodeSofaRequest((SofaRequest) object, context);
        } else if (object instanceof SofaResponse) {
            return encodeSofaResponse((SofaResponse) object, context);
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

    /**
     * Do encode SofaRequest
     *
     * @param sofaRequest 请求
     * @param context     上下文
     * @return byte数据 abstract byte buf
     */
    protected AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context) {
        try {
            UnsafeByteArrayOutputStream outputStream = new UnsafeByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(outputStream);

            // 根据SerializeType信息决定序列化器
            boolean genericSerialize = context != null &&
                isGenericRequest(context.get(RemotingConstants.HEAD_GENERIC_TYPE));
            if (genericSerialize) {
                output.setSerializerFactory(genericSerializerFactory);
            } else {
                output.setSerializerFactory(serializerFactory);
            }

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

    /**
     * Do encode SofaResponse
     *
     * @param response 响应
     * @param context  上下文
     * @return byte数据 abstract byte buf
     */
    protected AbstractByteBuf encodeSofaResponse(SofaResponse response, Map<String, String> context) {
        try {
            UnsafeByteArrayOutputStream byteArray = new UnsafeByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(byteArray);
            output.setSerializerFactory(serializerFactory);
            output.writeObject(response);
            output.close();
            return new ByteStreamWrapperByteBuf(byteArray);
        } catch (IOException e) {
            throw buildSerializeError(e.getMessage(), e);
        }
    }

    @Override
    public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {
        if (clazz == null) {
            throw buildDeserializeError("class is null!");
        } else if (SofaRequest.class.equals(clazz)) {
            return decodeSofaRequest(data, context);
        } else if (SofaResponse.class.equals(clazz)) {
            return decodeSofaResponse(data, context);
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

    @Override
    public void decode(AbstractByteBuf data, Object template, Map<String, String> context) throws SofaRpcException {
        if (template == null) {
            throw buildDeserializeError("template is null!");
        } else if (template instanceof SofaRequest) {
            decodeSofaRequestByTemplate(data, context, (SofaRequest) template);
        } else if (template instanceof SofaResponse) {
            decodeSofaResponseByTemplate(data, context, (SofaResponse) template);
        } else {
            throw buildDeserializeError("Only support decode from SofaRequest and SofaResponse template");
        }
    }

    /**
     * Do decode  SofaRequest
     *
     * @param data    AbstractByteBuf
     * @param context 上下文
     * @return 请求 sofa request
     * @throws SofaRpcException 序列化出现异常
     */
    protected SofaRequest decodeSofaRequest(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
        try {
            UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
            Hessian2Input input = new Hessian2Input(inputStream);
            input.setSerializerFactory(serializerFactory);
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

    /**
     * Do decode  SofaRequest
     *
     * @param data     AbstractByteBuf
     * @param context  上下文
     * @param template SofaRequest template
     * @throws SofaRpcException 序列化出现异常
     */
    protected void decodeSofaRequestByTemplate(AbstractByteBuf data, Map<String, String> context,
                                               SofaRequest template) throws SofaRpcException {
        try {
            UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(data.array());
            Hessian2Input input = new Hessian2Input(inputStream);
            input.setSerializerFactory(serializerFactory);
            Object object = input.readObject();
            SofaRequest tmp = (SofaRequest) object;
            String targetServiceName = tmp.getTargetServiceUniqueName();
            if (targetServiceName == null) {
                throw buildDeserializeError("Target service name of request is null!");
            }
            // copy values to template
            template.setMethodName(tmp.getMethodName());
            template.setMethodArgSigs(tmp.getMethodArgSigs());
            template.setTargetServiceUniqueName(tmp.getTargetServiceUniqueName());
            template.setTargetAppName(tmp.getTargetAppName());
            template.addRequestProps(tmp.getRequestProps());

            String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetServiceName);
            template.setInterfaceName(interfaceName);

            // decode args
            String[] sig = template.getMethodArgSigs();
            Class<?>[] classSig = ClassTypeUtils.getClasses(sig);
            final Object[] args = new Object[sig.length];
            for (int i = 0; i < template.getMethodArgSigs().length; ++i) {
                args[i] = input.readObject(classSig[i]);
            }
            template.setMethodArgs(args);
            input.close();
        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage(), e);
        }
    }

    /**
     * Do decode SofaResponse
     *
     * @param data    AbstractByteBuf
     * @param context 上下文
     * @return 响应 sofa response
     * @throws SofaRpcException 序列化出现异常
     */
    protected SofaResponse decodeSofaResponse(AbstractByteBuf data, Map<String, String> context)
        throws SofaRpcException {
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

    /**
     * Do decode SofaResponse
     *
     * @param data     AbstractByteBuf
     * @param context  上下文
     * @param template SofaResponse template
     * @throws SofaRpcException 序列化出现异常
     */
    protected void decodeSofaResponseByTemplate(AbstractByteBuf data, Map<String, String> context,
                                                SofaResponse template) throws SofaRpcException {
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
