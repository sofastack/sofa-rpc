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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    private ObjectMapper        mapper                        = new ObjectMapper();

    private JacksonHelper       jacksonHelper                 = new JacksonHelper();

    private static final String DESERIALIZATIONFEATURE_PREFIX = "sofa.rpc.codec.jackson.DeserializationFeature.";

    private static final String SERIALIZATIONFEATURE_PREFIX   = "sofa.rpc.codec.jackson.SerializationFeature.";

    public JacksonSerializer() {

        Properties properties = System.getProperties();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(DESERIALIZATIONFEATURE_PREFIX)) {
                String enumName = StringUtils.substringAfter(key, DESERIALIZATIONFEATURE_PREFIX);
                for (DeserializationFeature df : DeserializationFeature.values()) {
                    if (df.name().equals(enumName)) {
                        boolean state = Boolean.parseBoolean(properties.getProperty(key));
                        mapper.configure(df, state);
                        break;
                    }
                }
            }
            if (key.startsWith(SERIALIZATIONFEATURE_PREFIX)) {
                String enumName = StringUtils.substringAfter(key, SERIALIZATIONFEATURE_PREFIX);
                for (SerializationFeature sf : SerializationFeature.values()) {
                    if (sf.name().equals(enumName)) {
                        boolean state = Boolean.parseBoolean(properties.getProperty(key));
                        mapper.configure(sf, state);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public AbstractByteBuf encode(Object object, Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        } else if (object instanceof SofaRequest) {
            return encodeSofaRequest((SofaRequest) object, context);
        } else if (object instanceof SofaResponse) {
            return encodeSofaResponse((SofaResponse) object, context);
        } else {
            try {
                return new ByteArrayWrapperByteBuf(mapper.writeValueAsBytes(object));
            } catch (JsonProcessingException e) {
                throw buildSerializeError(e.getMessage());
            }
        }
    }

    protected AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context)
        throws SofaRpcException {
        Object[] args = sofaRequest.getMethodArgs();
        if (args.length == 1) {
            return encode(args[0], context);
        } else {
            return encode(args, context);
        }
    }

    protected AbstractByteBuf encodeSofaResponse(SofaResponse sofaResponse, Map<String, String> context)
        throws SofaRpcException {
        AbstractByteBuf byteBuf;
        if (sofaResponse.isError()) {
            // rpc exception：error when body is illegal string
            byteBuf = encode(sofaResponse.getErrorMsg(), context);
        } else {
            //ok: when json can be deserialize correctly.
            Object appResponse = sofaResponse.getAppResponse();
            if (appResponse instanceof Throwable) {
                // biz exception：error when body is illegal string
                byteBuf = encode(((Throwable) appResponse).getMessage(), context);
            } else {
                byteBuf = encode(appResponse, context);
            }
        }
        return byteBuf;
    }

    @Override
    public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {

        Object result = null;

        if (clazz == null) {
            throw buildDeserializeError("class is null!");
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
        // 解析request信息
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
        JavaType[] requestClassList = jacksonHelper.getReqClass(targetService, sofaRequest.getMethodName());
        Object[] reqList = decode(data, requestClassList);
        sofaRequest.setMethodArgs(reqList);
        sofaRequest.setMethodArgSigs(parseArgSigs(requestClassList));
    }

    private Object[] decode(AbstractByteBuf data, JavaType[] clazzList) throws SofaRpcException {

        if (clazzList == null || clazzList.length == 0) {
            return new Object[0];
        }

        Object[] args = new Object[clazzList.length];

        try {

            JsonNode node = mapper.readTree(data.array());

            // json data is json arry
            if (node.isArray()) {
                // first parameter is Array or Collection Type
                if (clazzList.length == 1) {
                    if (!clazzList[0].isCollectionLikeType() && !clazzList[0].isArrayType()) {
                        throw buildDeserializeError("JSON data can't be json array");
                    }
                    args[0] = mapper.readValue(node.traverse(), clazzList[0]);
                    return args;
                } else {
                    // if there is more than one parameter, but request json array size is not equal class type size.
                    if (clazzList.length != node.size()) {
                        throw buildDeserializeError("JSON Array size is not equal parameter size");
                    }

                    for (int i = 0; i < clazzList.length; i++) {
                        args[i] = mapper.readValue(node.get(i).traverse(), clazzList[i]);
                    }
                }

            } else {

                if (clazzList.length > 1) {
                    throw buildDeserializeError("JSON data must be json array");
                }

                // json is other type(eg. map object string int...)
                args[0] = mapper.readValue(node.traverse(), clazzList[0]);
            }

            return args;
        } catch (SofaRpcException e) {
            throw e;
        } catch (IOException e) {
            throw buildDeserializeError(e.getMessage());
        }

    }

    private String[] parseArgSigs(JavaType[] reqList) {
        List<String> argSigs = new ArrayList<String>();
        for (JavaType type : reqList) {
            argSigs.add(type.getRawClass().getName());
        }

        return argSigs.toArray(new String[argSigs.size()]);
    }

    private void parseRequestHeader(String key, Map<String, String> headerMap,
                                    SofaRequest sofaRequest) {
        Map<String, String> traceMap = new HashMap<String, String>(8);
        CodecUtils.treeCopyTo(key + ".", headerMap, traceMap, true);
        if (!traceMap.isEmpty()) {
            sofaRequest.addRequestProp(key, traceMap);
        }
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
            // according interface and method name to find paramter types
            JavaType respType = jacksonHelper.getResClass(targetService, methodName);
            Object result;
            try {
                result = mapper.readValue(data.array(), respType);
            } catch (IOException e) {
                throw buildDeserializeError(e.getMessage());
            }
            sofaResponse.setAppResponse(result);
        }
    }
}
