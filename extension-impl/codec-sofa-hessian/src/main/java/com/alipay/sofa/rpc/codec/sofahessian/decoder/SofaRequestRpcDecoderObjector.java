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
package com.alipay.sofa.rpc.codec.sofahessian.decoder;

import com.alipay.hessian.ClassNameResolver;
import com.alipay.hessian.NameBlackListFilter;
import com.alipay.sofa.rpc.codec.RpcDecoderObjector;
import com.alipay.sofa.rpc.codec.sofahessian.BlackListFileLoader;
import com.alipay.sofa.rpc.codec.sofahessian.GenericMultipleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.sofahessian.GenericSingleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.sofahessian.MultipleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.sofahessian.SingleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.SerializerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author bystander
 * @version $Id: SofaRequestRpcDecoderObjector.java, v 0.1 2018年09月07日 4:34 PM bystander Exp $
 */
public class SofaRequestRpcDecoderObjector implements RpcDecoderObjector<SofaRequest> {

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
    public SofaRequestRpcDecoderObjector() {
        boolean enableMultipleClassLoader = RpcConfigs.getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE);
        serializerFactory = getSerializerFactory(enableMultipleClassLoader, false);
        genericSerializerFactory = getSerializerFactory(enableMultipleClassLoader, true);
        if (RpcConfigs.getBooleanValue(RpcOptions.SERIALIZE_BLACKLIST_ENABLE) &&
            SofaConfigs.getBooleanValue(SofaOptions.CONFIG_SERIALIZE_BLACKLIST, true)) {
            ClassNameResolver resolver = new ClassNameResolver();
            resolver.addFilter(new NameBlackListFilter(BlackListFileLoader.SOFA_SERIALIZE_BLACK_LIST, 8192));
            serializerFactory.setClassNameResolver(resolver);
            genericSerializerFactory.setClassNameResolver(resolver);
        }
    }

    @Override
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context, SofaRequest template)
        throws SofaRpcException {
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

    @Override
    public SofaRequest decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException {
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
     * Is generic response boolean.
     *
     * @param serializeType the serialize type
     * @return the boolean
     */
    protected boolean isGenericResponse(String serializeType) {
        return serializeType != null && serializeType.equals(RemotingConstants.SERIALIZE_FACTORY_GENERIC);
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

    protected SofaRpcException buildDeserializeError(String message) {
        return new SofaRpcException(getErrorCode(false), message);
    }

}