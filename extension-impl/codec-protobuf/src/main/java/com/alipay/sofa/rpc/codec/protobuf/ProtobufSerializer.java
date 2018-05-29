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
package com.alipay.sofa.rpc.codec.protobuf;

import com.alipay.sofa.rpc.codec.AbstractSerializer;
import com.alipay.sofa.rpc.codec.common.StringSerializer;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Protobuf serializer.
 * <p>
 * Encode: : Support MessageLite, String, SofaRequest and SofaResponse.
 * <p>
 * Decode by class mode : Support MessageLite and String.
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
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension(value = "protobuf", code = 11)
public class ProtobufSerializer extends AbstractSerializer {

    private final ProtobufHelper protobufHelper     = new ProtobufHelper();

    /**
     * Encode method name
     */
    private static final String  METHOD_TOBYTEARRAY = "toByteArray";
    /**
     * Decode method name
     */
    private static final String  METHOD_PARSEFROM   = "parseFrom";

    @Override
    public AbstractByteBuf encode(Object object, Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        } else if (object instanceof SofaRequest) {
            return encodeSofaRequest((SofaRequest) object, context);
        } else if (object instanceof SofaResponse) {
            return encodeSofaResponse((SofaResponse) object, context);
        } else if (protobufHelper.isProtoBufMessageObject(object)) {
            Class clazz = object.getClass();
            Method method = protobufHelper.toByteArrayMethodMap.get(clazz);
            if (method == null) {
                try {
                    method = clazz.getMethod(METHOD_TOBYTEARRAY);
                    method.setAccessible(true);
                    protobufHelper.toByteArrayMethodMap.put(clazz, method);
                } catch (Exception e) {
                    throw buildSerializeError("Cannot found method " + clazz.getName()
                        + ".toByteArray(), please check the generated code.", e);
                }
            }
            try {
                return new ByteArrayWrapperByteBuf((byte[]) method.invoke(object));
            } catch (Exception e) {
                throw buildSerializeError("Error when invoke " + clazz.getName() + ".toByteArray().", e);
            }
        } else if (object instanceof String) {
            return new ByteArrayWrapperByteBuf(StringSerializer.encode((String) object));
        } else {
            throw buildSerializeError("Unsupported class:" + object.getClass().getName()
                + ", only support protobuf message");
        }
    }

    protected AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context)
        throws SofaRpcException {
        Object[] args = sofaRequest.getMethodArgs();
        if (args.length > 1) {
            throw buildSerializeError("Protobuf only support one parameter!");
        }
        return encode(args[0], context);
    }

    protected AbstractByteBuf encodeSofaResponse(SofaResponse sofaResponse, Map<String, String> context)
        throws SofaRpcException {
        AbstractByteBuf byteBuf;
        if (sofaResponse.isError()) {
            // 框架异常：错误则body序列化的是错误字符串
            byteBuf = encode(sofaResponse.getErrorMsg(), context);
        } else {
            // 正确返回则解析序列化的protobuf返回对象
            Object appResponse = sofaResponse.getAppResponse();
            if (appResponse instanceof Throwable) {
                // 业务异常序列化的是错误字符串
                byteBuf = encode(((Throwable) appResponse).getMessage(), context);
            } else {
                byteBuf = encode(appResponse, context);
            }
        }
        return byteBuf;
    }

    @Override
    public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {
        if (clazz == null) {
            throw buildDeserializeError("class is null!");
        } else if (protobufHelper.isProtoBufMessageClass(clazz)) {
            if (data == null || data.readableBytes() == 0) {
                try {
                    Constructor constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw buildDeserializeError("Error when invoke " + clazz.getName() + ".newInstance().", e);
                }
            } else {
                Method method = protobufHelper.parseFromMethodMap.get(clazz);
                if (method == null) {
                    try {
                        method = clazz.getMethod(METHOD_PARSEFROM, byte[].class);
                        if (!Modifier.isStatic(method.getModifiers())) {
                            throw buildDeserializeError("Cannot found static method " + clazz.getName()
                                + ".parseFrom(byte[]), please check the generated code");
                        }
                        method.setAccessible(true);
                        protobufHelper.parseFromMethodMap.put(clazz, method);
                    } catch (NoSuchMethodException e) {
                        throw buildDeserializeError("Cannot found method " + clazz.getName()
                            + ".parseFrom(byte[]), please check the generated code", e);
                    }
                }
                try {
                    return method.invoke(null, data.array());
                } catch (Exception e) {
                    throw buildDeserializeError("Error when invoke " + clazz.getName() + ".parseFrom(byte[]).", e);
                }
            }
        } else if (clazz == String.class) {
            return StringSerializer.decode(data.array());
        } else {
            throw buildDeserializeError("Unsupported class:" + clazz.getName() + ", only support protobuf message");
        }
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

        // 解析tracer等信息
        parseRequestHeader(RemotingConstants.RPC_TRACE_NAME, head, sofaRequest);
        if (RpcInvokeContext.isBaggageEnable()) {
            parseRequestHeader(RemotingConstants.RPC_REQUEST_BAGGAGE, head, sofaRequest);
        }
        for (Map.Entry<String, String> entry : head.entrySet()) {
            sofaRequest.addRequestProp(entry.getKey(), entry.getValue());
        }

        // 根据接口+方法名找到参数类型 此处要处理byte[]为空的吗
        Class requestClass = protobufHelper.getReqClass(targetService,
            sofaRequest.getMethodName());
        Object pbReq = decode(data, requestClass, head);
        sofaRequest.setMethodArgs(new Object[] { pbReq });
        sofaRequest.setMethodArgSigs(new String[] { requestClass.getName() });
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
            // 根据接口+方法名找到参数类型
            Class responseClass = protobufHelper.getResClass(targetService, methodName);
            Object pbRes = decode(data, responseClass, head);
            sofaResponse.setAppResponse(pbRes);
        }
    }
}
