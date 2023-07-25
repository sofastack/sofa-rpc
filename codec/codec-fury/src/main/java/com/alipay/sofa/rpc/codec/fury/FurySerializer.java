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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
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

import io.fury.Fury;
import io.fury.Language;
import io.fury.serializer.CompatibleMode;

/**
 * @author lipan
 */
@Extension(value = "fury", code = 20)
public class FurySerializer extends AbstractSerializer {

    private final FuryHelper    furyHelper  = new FuryHelper();
    private final Set<Class<?>> registerSet = new ConcurrentHashSet<>();
    private final Fury          fury;

    public FurySerializer() {
        fury = Fury.builder()
            .withLanguage(Language.JAVA)
            .withRefTracking(true)
            .withCodegen(true)
            .withNumberCompressed(true)
            .withCompatibleMode(CompatibleMode.COMPATIBLE)
            .requireClassRegistration(false)
            .withClassLoader(getClass().getClassLoader())
            .withAsyncCompilationEnabled(true)
            .build();
    }

    @Override
    public AbstractByteBuf encode(final Object object, final Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        } else if (object instanceof SofaRequest) {
            return encodeSofaRequest((SofaRequest) object, context);
        } else if (object instanceof SofaResponse) {
            return encodeSofaResponse((SofaResponse) object, context);
        } else {
            Class<?> clazz = object.getClass();
            registerClass(clazz);
            return new ByteArrayWrapperByteBuf(fury.serialize(object));
        }
    }

    private void registerClass(Class<?> clazz) {
        if (!registerSet.contains(clazz)) {
            fury.register(clazz);
            registerSet.add(clazz);
        }
    }

    private AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context)
        throws SofaRpcException {
        Object[] args = sofaRequest.getMethodArgs();
        if (args.length > 1) {
            throw buildSerializeError("Fury only support one parameter!");
        }
        return encode(args[0], context);
    }

    private AbstractByteBuf encodeSofaResponse(SofaResponse sofaResponse, Map<String, String> context)
        throws SofaRpcException {
        AbstractByteBuf byteBuf;
        if (sofaResponse.isError()) {
            byteBuf = encode(sofaResponse.getErrorMsg(), context);
        } else {
            Object appResponse = sofaResponse.getAppResponse();
            if (appResponse instanceof Throwable) {
                byteBuf = encode(((Throwable) appResponse).getMessage(), context);
            } else {
                byteBuf = encode(appResponse, context);
            }
        }
        return byteBuf;
    }

    @Override
    public Object decode(final AbstractByteBuf data, final Class clazz, final Map<String, String> context)
        throws SofaRpcException {
        Object result = null;
        if (clazz == null) {
            throw buildDeserializeError("class is null!");
        } else if (data.readableBytes() <= 0) {
            try {
                result = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw buildDeserializeError(e.getMessage());
            }
            return result;
        } else {
            result = fury.deserialize(data.array());
        }
        return result;
    }

    @Override
    public void decode(final AbstractByteBuf data, final Object template, final Map<String, String> context)
        throws SofaRpcException {
        if (template == null) {
            throw buildDeserializeError("template is null!");
        } else if (template instanceof SofaRequest) {
            decodeSofaRequest(data, (SofaRequest) template, context);
        } else if (template instanceof SofaResponse) {
            decodeSofaResponse(data, (SofaResponse) template, context);
        } else {
            throw buildDeserializeError("Only support decode from SofaRequest and SofaResponse template");
        }

    }

    private void decodeSofaRequest(AbstractByteBuf data, SofaRequest sofaRequest, Map<String, String> head) {
        if (head == null) {
            throw buildDeserializeError("head is null!");
        }
        String targetService = head.remove(RemotingConstants.HEAD_TARGET_SERVICE);
        if (targetService != null) {
            sofaRequest.setTargetServiceUniqueName(targetService);
            String interfaceName = ConfigUniqueNameGenerator.getInterfaceName(targetService);
            sofaRequest.setInterfaceName(interfaceName);
        } else {
            throw buildDeserializeError("HEAD_TARGET_SERVICE is null");
        }
        String methodName = head.remove(RemotingConstants.HEAD_METHOD_NAME);
        if (methodName != null) {
            sofaRequest.setMethodName(methodName);
        } else {
            throw buildDeserializeError("HEAD_METHOD_NAME is null");
        }
        String targetApp = head.remove(RemotingConstants.HEAD_TARGET_APP);
        if (targetApp != null) {
            sofaRequest.setTargetAppName(targetApp);
        }

        parseRequestHeader(RemotingConstants.RPC_TRACE_NAME, head, sofaRequest);
        if (RpcInvokeContext.isBaggageEnable()) {
            parseRequestHeader(RemotingConstants.RPC_REQUEST_BAGGAGE, head, sofaRequest);
        }
        for (Map.Entry<String, String> entry : head.entrySet()) {
            sofaRequest.addRequestProp(entry.getKey(), entry.getValue());
        }

        Class requestClass = furyHelper.getReqClass(targetService,
            sofaRequest.getMethodName());
        Object pbReq = decode(data, requestClass, head);
        sofaRequest.setMethodArgs(new Object[] { pbReq });
        sofaRequest.setMethodArgSigs(new String[] { requestClass.getName() });
    }

    private void decodeSofaResponse(AbstractByteBuf data, SofaResponse sofaResponse, Map<String, String> head) {
        if (head == null) {
            throw buildDeserializeError("head is null!");
        }
        String targetService = head.remove(RemotingConstants.HEAD_TARGET_SERVICE);
        if (targetService == null) {
            throw buildDeserializeError("HEAD_TARGET_SERVICE is null");
        }
        String methodName = head.remove(RemotingConstants.HEAD_METHOD_NAME);
        if (methodName == null) {
            throw buildDeserializeError("HEAD_METHOD_NAME is null");
        }

        boolean isError = false;
        if (StringUtils.TRUE.equals(head.remove(RemotingConstants.HEAD_RESPONSE_ERROR))) {
            isError = true;
        }
        if (!head.isEmpty()) {
            sofaResponse.setResponseProps(head);
        }
        if (isError) {
            String errorMessage = (String) decode(data, String.class, head);
            sofaResponse.setErrorMsg(errorMessage);
        } else {
            // 根据接口+方法名找到参数类型
            Class responseClass = furyHelper.getResClass(targetService, methodName);
            Object pbRes = decode(data, responseClass, head);
            sofaResponse.setAppResponse(pbRes);
        }
    }

    private void parseRequestHeader(String key, Map<String, String> headerMap,
                                    SofaRequest sofaRequest) {
        Map<String, String> traceMap = new HashMap<String, String>(8);
        CodecUtils.treeCopyTo(key + ".", headerMap, traceMap, true);
        if (!traceMap.isEmpty()) {
            sofaRequest.addRequestProp(key, traceMap);
        }
    }

}
