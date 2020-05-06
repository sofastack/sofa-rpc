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
package com.alipay.sofa.rpc.codec.msgpack;

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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.msgpack.MessagePack;

/**
 * @author leyou240
 * @create 2019/10/12 18:04
 */
@Extension(value = "msgpack", code = 13)
public class MsgPackSerializer extends AbstractSerializer {
    private final MessagePack messagePack = new MessagePack();
    private final MsgPackHelper helper = new MsgPackHelper();
    private final Set<Class<?>> registerSet = new ConcurrentHashSet<>();

    @Override
    public AbstractByteBuf encode(Object object, Map<String, String> context) throws SofaRpcException {
        if (object == null) {
            throw buildSerializeError("Unsupported null message!");
        } else if (object instanceof SofaRequest) {
            return encodeSofaRequest((SofaRequest) object, context);
        } else if (object instanceof SofaResponse) {
            return encodeSofaResponse((SofaResponse) object, context);
        } else if (helper.isJavaClass(object)) {
            //jdk原始类型
            try {
                return new ByteArrayWrapperByteBuf(messagePack.write(object));
            } catch (IOException e) {
                throw buildSerializeError(e.getMessage());
            }
        } else {
            try {
                Class<?> clazz = object.getClass();
                registerClass(clazz);
                return new ByteArrayWrapperByteBuf(messagePack.write(object));
            } catch (IOException e) {
                throw buildSerializeError(e.getMessage());
            }
        }
    }

    /**
     * 注册class到messagePack
     *
     * @param clazz
     */
    private void registerClass(Class<?> clazz) {
        if (!registerSet.contains(clazz)) {
            messagePack.register(clazz);
            registerSet.add(clazz);
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
            } catch (InstantiationException | IllegalAccessException e) {
                throw buildDeserializeError(e.getMessage());
            }
            return result;
        } else if (helper.isJavaClass(clazz)) {
            //jdk原始类型
            try {
                return messagePack.read(data.array(), clazz);
            } catch (IOException e) {
                throw buildDeserializeError(e.getMessage());
            }
        } else {
            try {
                registerClass(clazz);
                return messagePack.read(data.array(), clazz);
            } catch (IOException e) {
                throw buildDeserializeError(e.getMessage());
            }
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

    protected AbstractByteBuf encodeSofaRequest(SofaRequest sofaRequest, Map<String, String> context)
            throws SofaRpcException {
        Object[] args = sofaRequest.getMethodArgs();
        if (args.length > 1) {
            throw buildSerializeError("Msgpack only support one parameter!");
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
            // 正确返回则解析序列化的Msgpack返回对象
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

        // according interface and method name to find paramter types
        Class requestClass = helper.getReqClass(targetService,
                sofaRequest.getMethodName());

        Object pbReq = decode(data, requestClass, head);
        sofaRequest.setMethodArgs(new Object[]{pbReq});
        sofaRequest.setMethodArgSigs(new String[]{requestClass.getName()});
    }

    private void parseRequestHeader(String key, Map<String, String> headerMap,
                                    SofaRequest sofaRequest) {
        Map<String, String> traceMap = new HashMap<>(8);
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
            Class responseClass = helper.getResClass(targetService, methodName);
            Object pbRes = decode(data, responseClass, head);
            sofaResponse.setAppResponse(pbRes);
        }
    }
}
