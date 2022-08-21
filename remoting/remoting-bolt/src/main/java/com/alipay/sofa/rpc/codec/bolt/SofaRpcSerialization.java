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
package com.alipay.sofa.rpc.codec.bolt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.CodecUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_RPC_REQUEST_COMMAND;

/**
 * Sofa RPC BOLT 协议的对象序列化/反序列化自定义类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @author <a href=mailto:hongwei.yhw@antfin.com>HongWei Yi</a>
 */
public class SofaRpcSerialization extends DefaultCustomSerializer {

    private static final Logger   LOGGER = LoggerFactory.getLogger(SofaRpcSerialization.class);

    protected SimpleMapSerializer mapSerializer;

    public SofaRpcSerialization() {
        init();
    }

    /**
     * Init this custom serializer
     */
    protected void init() {
        mapSerializer = new SimpleMapSerializer();
    }

    @Override
    public <Response extends ResponseCommand> boolean serializeHeader(Response response)
        throws SerializationException {
        if (response instanceof RpcResponseCommand) {
            RpcInternalContext.getContext().getStopWatch().tick();

            Object responseObject = ((RpcResponseCommand) response).getResponseObject();
            if (responseObject instanceof SofaResponse) {
                SofaResponse sofaResponse = (SofaResponse) responseObject;
                if (sofaResponse.isError() || sofaResponse.getAppResponse() instanceof Throwable) {
                    sofaResponse.addResponseProp(RemotingConstants.HEAD_RESPONSE_ERROR, StringUtils.TRUE);
                }
                byte[] header = null;
                try {
                    header = mapSerializer.encode(sofaResponse.getResponseProps());
                } catch (Exception e) {
                    String traceId = (String) RpcInternalContext.getContext().getAttachment("_trace_id");
                    String rpcId = (String) RpcInternalContext.getContext().getAttachment("_span_id");
                    LOGGER.error("traceId={}, rpcId={}, Response serializeHeader exception, msg={}", traceId, rpcId,
                        e.getMessage(), e);
                    throw new SerializationException(e.getMessage() + ", traceId=" + traceId + ", rpcId=" + rpcId, e);
                }
                response.setHeader(header);
            }
            return true;
        }
        return false;
    }

    @Override
    public <Request extends RequestCommand> boolean serializeHeader(Request request, InvokeContext invokeContext)
        throws SerializationException {
        if (request instanceof RpcRequestCommand) {
            RpcInternalContext.getContext().getStopWatch().tick();

            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            Object requestObject = requestCommand.getRequestObject();
            String service = getTargetServiceName(requestObject);
            if (StringUtils.isNotEmpty(service)) {
                Map<String, String> header = new HashMap<String, String>(16);
                header.put(RemotingConstants.HEAD_SERVICE, service);
                putRequestMetadataToHeader(requestObject, header);
                requestCommand.setHeader(mapSerializer.encode(header));
            }
            return true;
        }
        return false;
    }

    protected void putRequestMetadataToHeader(Object requestObject, Map<String, String> header) {
        if (requestObject instanceof RequestBase) {
            RequestBase requestBase = (RequestBase) requestObject;
            header.put(RemotingConstants.HEAD_METHOD_NAME, requestBase.getMethodName());
            header.put(RemotingConstants.HEAD_TARGET_SERVICE, requestBase.getTargetServiceUniqueName());

            if (requestBase instanceof SofaRequest) {
                SofaRequest sofaRequest = (SofaRequest) requestBase;
                header.put(RemotingConstants.HEAD_TARGET_APP, sofaRequest.getTargetAppName());
                Map<String, Object> requestProps = sofaRequest.getRequestProps();
                if (requestProps != null) {
                    // <String, Object> 转扁平化 <String, String>
                    CodecUtils.flatCopyTo("", requestProps, header);
                }
            }
        }
    }

    /**
     * Get target service name from request
     *
     * @param request Request object
     * @return service name
     */
    protected String getTargetServiceName(Object request) {
        if (request instanceof RequestBase) {
            RequestBase requestBase = (RequestBase) request;
            return requestBase.getTargetServiceUniqueName();
        }

        return null;
    }

    @Override
    public <Request extends RequestCommand> boolean deserializeHeader(Request request)
        throws DeserializationException {
        if (request instanceof RpcRequestCommand) {
            RpcInternalContext.getContext().getStopWatch().tick();

            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            if (requestCommand.getRequestHeader() != null) {
                // 代表已经提前解析过了，例如使用自定义业务线程池的时候，bolt会提前解析变长Header的数据
                return true;
            }
            byte[] header = requestCommand.getHeader();
            // 解析头部
            Map<String, String> headerMap = mapSerializer.decode(header);
            requestCommand.setRequestHeader(headerMap);
            RpcInvokeContext.getContext().put(RpcConstants.SOFA_REQUEST_HEADER_KEY,
                Collections.unmodifiableMap(headerMap));

            return true;
        }
        return false;
    }

    @Override
    public <Response extends ResponseCommand> boolean deserializeHeader(Response response, InvokeContext invokeContext)
        throws DeserializationException {
        if (response instanceof RpcResponseCommand) {
            RpcInternalContext.getContext().getStopWatch().tick();

            RpcResponseCommand responseCommand = (RpcResponseCommand) response;
            byte[] header = responseCommand.getHeader();
            responseCommand.setResponseHeader(mapSerializer.decode(header));
            return true;
        }
        return false;
    }

    @Override
    public <Request extends RequestCommand> boolean serializeContent(Request request, InvokeContext invokeContext)
        throws SerializationException {
        if (request instanceof RpcRequestCommand) {
            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            RpcInvokeContext.getContext().put(INTERNAL_KEY_RPC_REQUEST_COMMAND, requestCommand);
            Object requestObject = requestCommand.getRequestObject();
            byte serializerCode = requestCommand.getSerializer();
            long serializeStartTime = System.nanoTime();
            try {
                Map<String, String> header = (Map<String, String>) requestCommand.getRequestHeader();
                if (header == null) {
                    header = new HashMap<String, String>();
                }
                putKV(header, RemotingConstants.HEAD_GENERIC_TYPE,
                    (String) invokeContext.get(RemotingConstants.HEAD_GENERIC_TYPE));

                Serializer rpcSerializer = com.alipay.sofa.rpc.codec.SerializerFactory
                    .getSerializer(serializerCode);
                AbstractByteBuf byteBuf = rpcSerializer.encode(requestObject, header);
                request.setContent(byteBuf.array());
                return true;
            } catch (Exception ex) {
                throw new SerializationException(ex.getMessage(), ex);
            } finally {
                // R5：record request serialization time
                recordSerializeRequest(requestCommand, invokeContext, serializeStartTime);
                RpcInvokeContext.getContext().remove(INTERNAL_KEY_RPC_REQUEST_COMMAND);
            }
        }
        return false;
    }

    /**
     * 客户端记录序列化请求的耗时和
     *
     * @param requestCommand 请求对象
     */
    protected void recordSerializeRequest(RequestCommand requestCommand, InvokeContext invokeContext,
                                          long serializeStartTime) {
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_REQ_SERIALIZE_TIME_NANO,
            System.nanoTime() - serializeStartTime);
        if (!RpcInternalContext.isAttachmentEnable()) {
            return;
        }
        RpcInternalContext context = null;
        if (invokeContext != null) {
            // 客户端异步调用的情况下，上下文会放在InvokeContext中传递
            context = invokeContext.get(RemotingConstants.INVOKE_CTX_RPC_CTX);
        }
        if (context == null) {
            context = RpcInternalContext.getContext();
        }
        int cost = context.getStopWatch().tick().read();
        int requestSize = RpcProtocol.getRequestHeaderLength()
            + requestCommand.getClazzLength()
            + requestCommand.getContentLength()
            + requestCommand.getHeaderLength();
        // 记录请求序列化大小和请求序列化耗时
        context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, requestSize);
        context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SERIALIZE_TIME, cost);
    }

    @Override
    public <Request extends RequestCommand> boolean deserializeContent(Request request)
        throws DeserializationException {
        if (request instanceof RpcRequestCommand) {
            RpcRequestCommand requestCommand = (RpcRequestCommand) request;
            Object header = requestCommand.getRequestHeader();
            if (!(header instanceof Map)) {
                throw new DeserializationException("Head of request is null or is not map");
            }
            Map<String, String> headerMap = (Map<String, String>) header;
            String traceId = headerMap.get("rpc_trace_context.sofaTraceId");
            String rpcId = headerMap.get("rpc_trace_context.sofaRpcId");
            long deserializeStartTime = System.nanoTime();
            try {
                byte[] content = requestCommand.getContent();
                if (content == null || content.length == 0) {
                    throw new DeserializationException("Content of request is null");
                }
                String service = headerMap.get(RemotingConstants.HEAD_SERVICE);
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

                ClassLoader serviceClassLoader = ReflectCache.getServiceClassLoader(service);
                try {
                    Thread.currentThread().setContextClassLoader(serviceClassLoader);

                    Serializer rpcSerializer = com.alipay.sofa.rpc.codec.SerializerFactory
                        .getSerializer(requestCommand.getSerializer());
                    Object sofaRequest = ClassUtils.forName(requestCommand.getRequestClass()).newInstance();
                    rpcSerializer.decode(new ByteArrayWrapperByteBuf(requestCommand.getContent()),
                        sofaRequest, headerMap);

                    //for service mesh or other scene, we need to add more info from header
                    if (sofaRequest instanceof SofaRequest) {
                        setRequestPropertiesWithHeaderInfo(headerMap, (SofaRequest) sofaRequest);
                        parseRequestHeader(headerMap, (SofaRequest) sofaRequest);
                    }
                    requestCommand.setRequestObject(sofaRequest);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }

                return true;
            } catch (Exception ex) {
                LOGGER.error("traceId={}, rpcId={}, Request deserializeContent exception, msg={}", traceId, rpcId,
                    ex.getMessage(), ex);
                throw new DeserializationException(ex.getMessage() + ", traceId=" + traceId + ", rpcId=" + rpcId, ex);
            } finally {
                // R6：Record request deserialization time
                recordDeserializeRequest(requestCommand, deserializeStartTime);
            }
        }
        return false;
    }

    @VisibleForTesting
    protected void parseRequestHeader(Map<String, String> headerMap, SofaRequest sofaRequest) {
        // 处理 tracer
        parseRequestHeader(RemotingConstants.RPC_TRACE_NAME, headerMap, sofaRequest);
        Map<String, Object> requestProps = sofaRequest.getRequestProps();
        if (requestProps == null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                sofaRequest.addRequestProp(entry.getKey(), entry.getValue());
            }
        } else {
            replaceWithHeaderMap(headerMap, requestProps);
        }
    }

    private void parseRequestHeader(String key, Map<String, String> headerMap,
                                    SofaRequest sofaRequest) {
        Map<String, String> traceMap = new HashMap<String, String>();
        CodecUtils.treeCopyTo(key + ".", headerMap, traceMap, true);
        Object traceCtx = sofaRequest.getRequestProp(key);
        if (traceCtx == null) {
            sofaRequest.addRequestProp(key, traceMap);
        } else if (traceCtx instanceof Map) {
            ((Map<String, String>) traceCtx).putAll(traceMap);
        }
    }

    private void replaceWithHeaderMap(Map<String, String> headerMap, Map props) {
        if (headerMap == null || props == null) {
            return;
        }
        // 1. 如果 key 已经在requestProps存在，value 为 String 时进行覆盖
        // 2. header 中的 value 为 Blank 时不进行覆盖
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            Object o = props.get(entry.getKey());
            if (o == null) {
                props.put(entry.getKey(), entry.getValue());
            } else if (o instanceof String) {
                if (StringUtils.isBlank((CharSequence) o)
                    || StringUtils.isNotBlank(entry.getValue())) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 服务端记录反序列化请求的大小和耗时
     *
     * @param requestCommand 请求对象
     */
    private void recordDeserializeRequest(RequestCommand requestCommand, long deserializeStartTime) {
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_REQ_DESERIALIZE_TIME_NANO, System.nanoTime() -
            deserializeStartTime);
        if (!RpcInternalContext.isAttachmentEnable()) {
            return;
        }
        RpcInternalContext context = RpcInternalContext.getContext();
        int cost = context.getStopWatch().tick().read();
        int requestSize = RpcProtocol.getRequestHeaderLength()
            + requestCommand.getClazzLength()
            + requestCommand.getContentLength()
            + requestCommand.getHeaderLength();
        // 记录请求反序列化大小和请求反序列化耗时
        context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, requestSize);
        context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_DESERIALIZE_TIME, cost);
    }

    @Override
    public <Response extends ResponseCommand> boolean serializeContent(Response response)
        throws SerializationException {
        if (response instanceof RpcResponseCommand) {
            RpcResponseCommand responseCommand = (RpcResponseCommand) response;
            byte serializerCode = response.getSerializer();
            long serializeStartTime = System.nanoTime();
            try {
                Serializer rpcSerializer = com.alipay.sofa.rpc.codec.SerializerFactory.getSerializer(serializerCode);
                AbstractByteBuf byteBuf = rpcSerializer.encode(responseCommand.getResponseObject(), null);
                responseCommand.setContent(byteBuf.array());
                return true;
            } catch (Exception ex) {
                String traceId = (String) RpcInternalContext.getContext().getAttachment("_trace_id");
                String rpcId = (String) RpcInternalContext.getContext().getAttachment("_span_id");
                LOGGER.error("traceId={}, rpcId={}, Response serializeContent exception, msg = {}", traceId, rpcId,
                    ex.getMessage(), ex);
                throw new SerializationException(ex.getMessage() + ", traceId=" + traceId + ", rpcId=" + rpcId, ex);
            } finally {
                // R6：Record response serialization time
                recordSerializeResponse(responseCommand, serializeStartTime);
            }
        }
        return false;
    }

    /**
     * 服务端记录序列化响应的大小和耗时
     *
     * @param responseCommand 响应体
     */
    private void recordSerializeResponse(RpcResponseCommand responseCommand, long serializeStartTime) {
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_RESP_SERIALIZE_TIME_NANO, System.nanoTime() -
            serializeStartTime);
        if (!RpcInternalContext.isAttachmentEnable()) {
            return;
        }
        RpcInternalContext context = RpcInternalContext.getContext();
        int cost = context.getStopWatch().tick().read();
        int respSize = RpcProtocol.getResponseHeaderLength()
            + responseCommand.getClazzLength()
            + responseCommand.getContentLength()
            + responseCommand.getHeaderLength();
        // 记录响应序列化大小和请求序列化耗时
        context.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, respSize);
        context.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SERIALIZE_TIME, cost);
    }

    @Override
    public <Response extends ResponseCommand> boolean deserializeContent(Response response, InvokeContext invokeContext)
        throws DeserializationException {
        if (response instanceof RpcResponseCommand) {
            RpcResponseCommand responseCommand = (RpcResponseCommand) response;
            byte serializer = response.getSerializer();
            byte[] content = responseCommand.getContent();
            if (content == null || content.length == 0) {
                return false;
            }
            long deserializeStartTime = System.nanoTime();

            try {
                Object sofaResponse = ClassUtils.forName(responseCommand.getResponseClass()).newInstance();

                Map<String, String> header = (Map<String, String>) responseCommand.getResponseHeader();
                if (header == null) {
                    header = new HashMap<String, String>();
                }
                putKV(header, RemotingConstants.HEAD_TARGET_SERVICE,
                    (String) invokeContext.get(RemotingConstants.HEAD_TARGET_SERVICE));
                putKV(header, RemotingConstants.HEAD_METHOD_NAME,
                    (String) invokeContext.get(RemotingConstants.HEAD_METHOD_NAME));
                putKV(header, RemotingConstants.HEAD_GENERIC_TYPE,
                    (String) invokeContext.get(RemotingConstants.HEAD_GENERIC_TYPE));

                Serializer rpcSerializer = com.alipay.sofa.rpc.codec.SerializerFactory.getSerializer(serializer);
                rpcSerializer.decode(new ByteArrayWrapperByteBuf(responseCommand.getContent()), sofaResponse, header);
                if (sofaResponse instanceof SofaResponse) {
                    parseResponseHeader(header, (SofaResponse) sofaResponse);
                }
                responseCommand.setResponseObject(sofaResponse);
                return true;
            } catch (Exception ex) {
                throw new DeserializationException(ex.getMessage(), ex);
            } finally {
                //R5：Record response deserialization time
                recordDeserializeResponse(responseCommand, invokeContext, deserializeStartTime);
            }
        }

        return false;
    }

    @VisibleForTesting
    protected void parseResponseHeader(Map<String, String> headerMap, SofaResponse sofaResponse) {
        // 处理 tracer
        Map<String, String> responseProps = sofaResponse.getResponseProps();
        if (responseProps == null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                sofaResponse.addResponseProp(entry.getKey(), entry.getValue());
            }
        } else {
            replaceWithHeaderMap(headerMap, responseProps);
        }
    }

    protected void putKV(Map<String, String> map, String key, String value) {
        if (map != null && key != null && value != null) {
            map.put(key, value);
        }
    }

    /**
     * 客户端记录响应反序列化大小和响应反序列化耗时
     *
     * @param responseCommand 响应体
     */
    private void recordDeserializeResponse(RpcResponseCommand responseCommand, InvokeContext invokeContext,
                                           long deserializeStartTime) {
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_RESP_DESERIALIZE_TIME_NANO, System.nanoTime() -
            deserializeStartTime);
        if (!RpcInternalContext.isAttachmentEnable()) {
            return;
        }
        RpcInternalContext context = null;
        if (invokeContext != null) {
            // 客户端异步调用的情况下，上下文会放在InvokeContext中传递
            context = invokeContext.get(RemotingConstants.INVOKE_CTX_RPC_CTX);
        }
        if (context == null) {
            context = RpcInternalContext.getContext();
        }
        int cost = context.getStopWatch().tick().read();
        int respSize = RpcProtocol.getResponseHeaderLength()
            + responseCommand.getClazzLength()
            + responseCommand.getContentLength()
            + responseCommand.getHeaderLength();
        // 记录响应反序列化大小和响应反序列化耗时
        context.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, respSize);
        context.setAttachment(RpcConstants.INTERNAL_KEY_RESP_DESERIALIZE_TIME, cost);
    }

    /**
     * 使用header中的值替换部分请求属性
     * @param headerMap header
     * @param request SofaRequest
     */
    protected void setRequestPropertiesWithHeaderInfo(Map<String, String> headerMap, SofaRequest request) {
        // Try to obtain the unique name of the target service from the headerMap.
        // Due to the MOSN routing logic, it may be different from the original service unique name.
        String headerService = headerMap.get(RemotingConstants.HEAD_SERVICE);
        if (headerService == null) {
            headerService = headerMap.get(RemotingConstants.HEAD_TARGET_SERVICE);
        }
        if (StringUtils.isNotBlank(headerService)) {
            request.setTargetServiceUniqueName(headerService);
        }
    }

}
