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
package com.alipay.sofa.rpc.codec.fory;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.codec.CustomSerializer;
import com.alipay.sofa.rpc.codec.common.BlackAndWhiteListFileLoader;
import com.alipay.sofa.rpc.codec.common.SerializeCheckStatus;
import com.alipay.sofa.rpc.codec.fory.serialize.SofaRequestForySerializer;
import com.alipay.sofa.rpc.codec.fory.serialize.SofaResponseForySerializer;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.apache.fory.Fory;
import org.apache.fory.ThreadLocalFory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.resolver.AllowListChecker;
import org.apache.fory.resolver.ClassResolver;

import java.util.List;
import java.util.Map;

import static org.apache.fory.config.CompatibleMode.COMPATIBLE;

/**
 * Apache Fory serializer for SOFARPC.
 * Uses the new Apache Fory dependency (org.apache.fory:fory-core)
 * instead of the legacy org.furyio:fury-core used by FurySerializer.
 *
 * @see com.alipay.sofa.rpc.codec.fury.FurySerializer
 */
@Extension(value = "fory", code = 23)
public class ForySerializer extends AbstractSerializer {

    protected final ThreadSafeFory fory;

    private final String           checkerMode = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_CHECKER_MODE);

    public ForySerializer() {
        fory = new ThreadLocalFory(classLoader -> {
            Fory foryInstance = Fory.builder()
                    .withLanguage(Language.JAVA)
                    .withRefTracking(true)
                    .withCodegen(true)
                    .withNumberCompressed(true)
                    .withCompatibleMode(COMPATIBLE)
                    .requireClassRegistration(false)
                    .withClassLoader(classLoader)
                    .withAsyncCompilation(true)
                    .build();

            // In Apache Fory 0.16.0, the security checker is set via
            // typeResolver.setTypeChecker(TypeChecker).
            // ClassResolver extends TypeResolver and is used for addListener.
            ClassResolver classResolver = (ClassResolver) foryInstance.getTypeResolver();

            if (checkerMode.equalsIgnoreCase(SerializeCheckStatus.DISABLE.name())) {
                AllowListChecker noChecker = new AllowListChecker(AllowListChecker.CheckLevel.DISABLE);
                classResolver.setTypeChecker(noChecker);
            } else if (checkerMode.equalsIgnoreCase(SerializeCheckStatus.WARN.name())) {
                AllowListChecker blackListChecker = new AllowListChecker(AllowListChecker.CheckLevel.WARN);
                classResolver.setTypeChecker(blackListChecker);
                blackListChecker.addListener(classResolver);
                List<String> blackList = BlackAndWhiteListFileLoader.SOFA_SERIALIZE_BLACK_LIST;
                for (String key : blackList) {
                    blackListChecker.disallowClass(key + "*");
                }
            } else {
                // Default: STRICT mode
                AllowListChecker blackAndWhiteListChecker = new AllowListChecker(AllowListChecker.CheckLevel.STRICT);
                classResolver.setTypeChecker(blackAndWhiteListChecker);
                blackAndWhiteListChecker.addListener(classResolver);
                List<String> whiteList = BlackAndWhiteListFileLoader.SOFA_SERIALIZER_WHITE_LIST;
                for (String key : whiteList) {
                    blackAndWhiteListChecker.allowClass(key + "*");
                }
                List<String> blackList = BlackAndWhiteListFileLoader.SOFA_SERIALIZE_BLACK_LIST;
                for (String key : blackList) {
                    blackAndWhiteListChecker.disallowClass(key + "*");
                }
            }

            foryInstance.register(SofaRequest.class);
            foryInstance.register(SofaResponse.class);
            foryInstance.register(SofaRpcException.class);
            return foryInstance;
        });
        addCustomSerializer(SofaRequest.class, new SofaRequestForySerializer(fory));
        addCustomSerializer(SofaResponse.class, new SofaResponseForySerializer(fory));
    }

    @Override
    public AbstractByteBuf encode(final Object object, final Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            fory.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getObjCustomSerializer(object);
            if (customSerializer != null) {
                return customSerializer.encodeObject(object, context);
            } else {
                MemoryBuffer writeBuffer = MemoryBuffer.newHeapBuffer(32);
                writeBuffer.writerIndex(0);
                fory.serialize(writeBuffer, object);
                return new ByteArrayWrapperByteBuf(writeBuffer.getBytes(0, writeBuffer.writerIndex()));
            }
        } catch (Exception e) {
            throw buildSerializeError(e.getMessage(), e);
        } finally {
            fory.clearClassLoader(contextClassLoader);
        }
    }

    @Override
    public Object decode(final AbstractByteBuf data, final Class clazz, final Map<String, String> context)
        throws SofaRpcException {
        if (data.readableBytes() <= 0 || clazz == null) {
            throw buildDeserializeError("Deserialized array is empty.");
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            fory.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getCustomSerializer(clazz);
            if (customSerializer != null) {
                return customSerializer.decodeObject(data, context);
            } else {
                MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
                return fory.deserialize(readBuffer);
            }
        } catch (Exception e) {
            throw buildDeserializeError(e.getMessage(), e);
        } finally {
            fory.clearClassLoader(contextClassLoader);
        }
    }

    @Override
    public void decode(final AbstractByteBuf data, final Object template, final Map<String, String> context)
        throws SofaRpcException {
        if (template == null) {
            throw buildDeserializeError("template is null!");
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            fory.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getObjCustomSerializer(template);
            if (customSerializer != null) {
                customSerializer.decodeObjectByTemplate(data, context, template);
            } else {
                throw buildDeserializeError("Only support decode from SofaRequest and SofaResponse template");
            }
        } catch (Exception e) {
            throw buildDeserializeError(e.getMessage(), e);
        } finally {
            fory.clearClassLoader(contextClassLoader);
        }
    }
}
