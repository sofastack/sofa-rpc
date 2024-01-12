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
package com.alipay.sofa.rpc.codec.fury;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.codec.CustomSerializer;
import com.alipay.sofa.rpc.codec.common.BlackAndWhiteListFileLoader;
import com.alipay.sofa.rpc.codec.common.SerializeCheckStatus;
import com.alipay.sofa.rpc.codec.fury.serialize.SofaRequestFurySerializer;
import com.alipay.sofa.rpc.codec.fury.serialize.SofaResponseFurySerializer;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import io.fury.Fury;
import io.fury.ThreadLocalFury;
import io.fury.ThreadSafeFury;
import io.fury.config.Language;
import io.fury.memory.MemoryBuffer;
import io.fury.resolver.AllowListChecker;

import java.util.List;
import java.util.Map;

import static io.fury.config.CompatibleMode.COMPATIBLE;

/**
 * @author lipan
 */
@Extension(value = "fury2", code = 22)
public class FurySerializer extends AbstractSerializer {

    protected final ThreadSafeFury fury;

    private final String           checkerMode = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_CHECKER_MODE);

    public FurySerializer() {
        fury = new ThreadLocalFury(classLoader -> {
            Fury f = Fury.builder().withLanguage(Language.JAVA)
                    .withRefTracking(true)
                    .withCodegen(true)
                    .withNumberCompressed(true)
                    .withCompatibleMode(COMPATIBLE)
                    .requireClassRegistration(false)
                    .withClassLoader(classLoader)
                    .withAsyncCompilation(true)
                    .build();

            // Do not use any configuration
            if (checkerMode.equalsIgnoreCase(SerializeCheckStatus.DISABLE.name())) {
                AllowListChecker noChecker = new AllowListChecker(AllowListChecker.CheckLevel.DISABLE);
                f.getClassResolver().setClassChecker(noChecker);
                return f;
            } else if (checkerMode.equalsIgnoreCase(SerializeCheckStatus.WARN.name())) {
                AllowListChecker blackListChecker = new AllowListChecker(AllowListChecker.CheckLevel.WARN);
                List<String> blackList = BlackAndWhiteListFileLoader.SOFA_SERIALIZE_BLACK_LIST;
                // To setting checker
                f.getClassResolver().setClassChecker(blackListChecker);
                blackListChecker.addListener(f.getClassResolver());
                // BlackList classes use wildcards
                for (String key : blackList) {
                    blackListChecker.disallowClass(key + "*");
                }
            } else if (checkerMode.equalsIgnoreCase(SerializeCheckStatus.STRICT.name())) {
                AllowListChecker blackAndWhiteListChecker = new AllowListChecker(AllowListChecker.CheckLevel.STRICT);
                List<String> whiteList = BlackAndWhiteListFileLoader.SOFA_SERIALIZER_WHITE_LIST;
                // To setting checker
                f.getClassResolver().setClassChecker(blackAndWhiteListChecker);
                blackAndWhiteListChecker.addListener(f.getClassResolver());
                // WhiteList classes use wildcards
                for (String key : whiteList) {
                    blackAndWhiteListChecker.allowClass(key + "*");
                }
                List<String> blackList = BlackAndWhiteListFileLoader.SOFA_SERIALIZE_BLACK_LIST;
                // To setting checker
                f.getClassResolver().setClassChecker(blackAndWhiteListChecker);
                blackAndWhiteListChecker.addListener(f.getClassResolver());
                // BlackList classes use wildcards
                for (String key : blackList) {
                    blackAndWhiteListChecker.disallowClass(key + "*");
                }
            }
            f.register(SofaRequest.class);
            f.register(SofaResponse.class);
            f.register(SofaRpcException.class);
            return f;
        });
        addCustomSerializer(SofaRequest.class, new SofaRequestFurySerializer(fury));
        addCustomSerializer(SofaResponse.class, new SofaResponseFurySerializer(fury));
    }

    @Override
    public AbstractByteBuf encode(final Object object, final Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            fury.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getObjCustomSerializer(object);
            if (customSerializer != null) {
                return customSerializer.encodeObject(object, context);
            } else {
                MemoryBuffer writeBuffer = MemoryBuffer.newHeapBuffer(32);
                writeBuffer.writerIndex(0);
                fury.serialize(writeBuffer, object);
                return new ByteArrayWrapperByteBuf(writeBuffer.getBytes(0, writeBuffer.writerIndex()));
            }
        } catch (Exception e) {
            throw buildSerializeError(e.getMessage(), e);
        } finally {
            fury.clearClassLoader(contextClassLoader);
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
            fury.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getCustomSerializer(clazz);
            if (customSerializer != null) {
                return customSerializer.decodeObject(data, context);
            } else {
                MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(data.array());
                return fury.deserialize(readBuffer);
            }
        } catch (Exception e) {
            throw buildDeserializeError(e.getMessage(), e);
        } finally {
            fury.clearClassLoader(contextClassLoader);
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
            fury.setClassLoader(contextClassLoader);
            CustomSerializer customSerializer = getObjCustomSerializer(template);
            if (customSerializer != null) {
                customSerializer.decodeObjectByTemplate(data, context, template);
            } else {
                throw buildDeserializeError("Only support decode from SofaRequest and SofaResponse template");
            }
        } catch (Exception e) {
            throw buildDeserializeError(e.getMessage(), e);
        } finally {
            fury.clearClassLoader(contextClassLoader);
        }
    }

}
