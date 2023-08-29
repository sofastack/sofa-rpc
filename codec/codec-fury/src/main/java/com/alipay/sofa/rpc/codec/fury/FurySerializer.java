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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.common.utils.CodecUtils;
import com.alipay.sofa.rpc.common.utils.FileUtils;
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
import io.fury.ThreadLocalFury;
import io.fury.serializer.CompatibleMode;

/**
 * @author lipan
 */
@Extension(value = "fury", code = 20)
public class FurySerializer extends AbstractSerializer {

    private final FuryHelper      furyHelper = new FuryHelper();

    private final ThreadLocalFury fury;

    public FurySerializer() {
        boolean WHITELIST = RpcConfigs.getBooleanValue(RpcOptions.FURY_SERIALIZER_WHITELIST);
        if (WHITELIST) {
            ArrayList<Class<?>> whiteList = new ArrayList<>();
            String json = null;
            try {
                json = FileUtils.file2String(FurySerializer.class, "/whiteList.json", "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Map<String, Boolean> map = JSON.parseObject(json, Map.class);
            map.forEach((className, use) -> {
                if (use) {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    whiteList.add(clazz);
                }

            });
            fury = new ThreadLocalFury(classLoader -> {

                Fury f = Fury.builder().withLanguage(Language.JAVA)
                    .withRefTracking(false)
                    .withCodegen(true)
                    .withNumberCompressed(true)
                    .withCompatibleMode(CompatibleMode.COMPATIBLE)
                    .requireClassRegistration(true)
                    .withClassLoader(classLoader)
                    .withAsyncCompilationEnabled(true)
                    .withSecureMode(true)
                    .build();
                if (!whiteList.isEmpty()) {
                    for (Class<?> clazz : whiteList) {
                        f.register(clazz);
                    }
                }
                return f;
            });

        } else {
            fury = new ThreadLocalFury(classLoader -> {

                return Fury.builder().withLanguage(Language.JAVA)
                    .withRefTracking(false)
                    .withCodegen(true)
                    .withNumberCompressed(true)
                    .withCompatibleMode(CompatibleMode.COMPATIBLE)
                    .requireClassRegistration(false)
                    .withClassLoader(classLoader)
                    .withAsyncCompilationEnabled(true)
                    .withSecureMode(false)
                    .build();
            });
        }

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
            return new ByteArrayWrapperByteBuf(fury.serialize(object));
        }
    }

    private AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context)
        throws SofaRpcException {
        Object[] args = sofaRequest.getMethodArgs();
        if (args.length == 1) {
            return encode(args[0], context);
        } else {
            return encode(args, context);
        }
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

        // parse tracer and baggage
        parseRequestHeader(RemotingConstants.RPC_TRACE_NAME, head, sofaRequest);
        if (RpcInvokeContext.isBaggageEnable()) {
            parseRequestHeader(RemotingConstants.RPC_REQUEST_BAGGAGE, head, sofaRequest);
        }
        for (Map.Entry<String, String> entry : head.entrySet()) {
            sofaRequest.addRequestProp(entry.getKey(), entry.getValue());
        }

        // according interface and method name to find parameter types
        Class[] requestClass = furyHelper.getReqClass(targetService,
            sofaRequest.getMethodName());
        Object[] pbReq = decode(data, requestClass, head);
        sofaRequest.setMethodArgs(pbReq);
        sofaRequest.setMethodArgSigs(parseArgSigs(requestClass));
    }

    private Object[] decode(final AbstractByteBuf data, final Class[] templateList, final Map<String, String> context)
        throws SofaRpcException {
        ArrayList<Object> objectList = new ArrayList<>();
        for (Class clazz : templateList) {
            Object result = null;
            if (clazz == null) {
                throw buildDeserializeError("class is null!");
            } else if (data.readableBytes() <= 0) {
                try {
                    result = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw buildDeserializeError(e.getMessage());
                }
            } else {
                result = fury.deserialize(data.array());
            }
            objectList.add(result);
        }
        return objectList.toArray(new Object[objectList.size()]);
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
            Class responseClass = furyHelper.getRespClass(targetService, methodName);
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

    private String[] parseArgSigs(Class[] reqList) {
        List<String> argSigs = new ArrayList<String>();
        for (Class type : reqList) {
            argSigs.add(type.getCanonicalName());
        }

        return argSigs.toArray(new String[argSigs.size()]);
    }

}
