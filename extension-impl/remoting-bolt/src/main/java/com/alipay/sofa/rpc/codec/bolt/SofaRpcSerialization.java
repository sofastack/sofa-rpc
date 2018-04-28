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

import com.alipay.hessian.ClassNameResolver;
import com.alipay.hessian.NameBlackListFilter;
import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcProtocol;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.sofa.rpc.codec.anthessian.BlackListFileLoader;
import com.alipay.sofa.rpc.codec.anthessian.GenericMultipleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.anthessian.GenericSingleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.anthessian.MultipleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.anthessian.SingleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.antpb.ProtobufSerializer;
import com.alipay.sofa.rpc.common.ReflectCache;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Sofa RPC BOLT 协议的对象序列化/反序列化自定义类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @author <a href=mailto:hongwei.yhw@antfin.com>HongWei Yi</a>
 */
public class SofaRpcSerialization extends DefaultCustomSerializer {

    private static final Logger   LOGGER = LoggerFactory.getLogger(SofaRpcSerialization.class);
    protected SerializerFactory   serializerFactory;
    protected SerializerFactory   genericSerializerFactory;
    protected SimpleMapSerializer mapSerializer;

    public SofaRpcSerialization() {
        init();
    }

    /**
     * Init this custom serializer
     */
    protected void init() {
        mapSerializer = new SimpleMapSerializer();
        if (RpcConfigs.getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE)) {
            serializerFactory = new MultipleClassLoaderSofaSerializerFactory();
            genericSerializerFactory = new GenericMultipleClassLoaderSofaSerializerFactory();
        } else {
            serializerFactory = new SingleClassLoaderSofaSerializerFactory();
            genericSerializerFactory = new GenericSingleClassLoaderSofaSerializerFactory();
        }
        if (RpcConfigs.getBooleanValue(RpcOptions.SERIALIZE_BLACKLIST_ENABLE) &&
            SofaConfigs.getBooleanValue(SofaOptions.CONFIG_SERIALIZE_BLACKLIST, true)) {
            ClassNameResolver resolver = new ClassNameResolver();
            resolver.addFilter(new NameBlackListFilter(BlackListFileLoader.SOFA_SERIALIZE_BLACK_LIST, 8192));
            serializerFactory.setClassNameResolver(resolver);
            genericSerializerFactory.setClassNameResolver(resolver);
        }
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
                    sofaResponse.addResponseProp(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
                }
                response.setHeader(mapSerializer.encode(sofaResponse.getResponseProps()));
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
                // 新序列化协议全部采用扁平化头部
                byte serializer = requestCommand.getSerializer();
                if (serializer != RemotingConstants.SERIALIZE_CODE_HESSIAN
                    && serializer != RemotingConstants.SERIALIZE_CODE_JAVA) {
                    putRequestMetadataToHeader(requestObject, header);
                }
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
                    ContextMapConverter.flatCopyTo("", requestProps, header);
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
            Object requestObject = requestCommand.getRequestObject();
            if (requestObject instanceof SofaRequest) {
                byte serializer = requestCommand.getSerializer();
                if (serializer == RemotingConstants.SERIALIZE_CODE_HESSIAN) {
                    try {
                        SofaRequest sofaRequest = (SofaRequest) requestObject;
                        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                        Hessian2Output output = new Hessian2Output(byteArray);

                        // 根据SerializeType信息决定序列化器
                        boolean genericSerialize = genericSerializeRequest(invokeContext);
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
                        request.setContent(byteArray.toByteArray());
                        byteArray.close();

                        return true;
                    } catch (IOException ex) {
                        throw new SerializationException(ex.getMessage(), ex);
                    } finally {
                        recordSerializeRequest(requestCommand, invokeContext);
                    }
                } else if (serializer == RemotingConstants.SERIALIZE_CODE_PROTOBUF) {
                    // protobuf序列化
                    try {
                        SofaRequest sofaRequest = (SofaRequest) requestObject;
                        Object[] args = sofaRequest.getMethodArgs();
                        if (args.length > 1) {
                            throw new SerializationException("Protobuf only support one parameter!");
                        }
                        ProtobufSerializer protobufSerializer = ProtobufSerializer.getInstance();
                        request.setContent(protobufSerializer.encode(args[0]));
                        return true;
                    } catch (SerializationException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SerializationException(e.getMessage(), e);
                    } finally {
                        recordSerializeRequest(requestCommand, invokeContext);
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 客户端记录序列化请求的耗时和
     *
     * @param requestCommand 请求对象
     */
    private void recordSerializeRequest(RequestCommand requestCommand, InvokeContext invokeContext) {
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
            byte serializer = requestCommand.getSerializer();
            if (serializer == RemotingConstants.SERIALIZE_CODE_HESSIAN) {
                byte[] content = requestCommand.getContent();
                if (content == null || content.length == 0) {
                    throw new DeserializationException("Content of request is null");
                }
                try {
                    ByteArrayInputStream input = new ByteArrayInputStream(requestCommand.getContent());
                    Hessian2Input hessianInput = new Hessian2Input(input);
                    hessianInput.setSerializerFactory(serializerFactory);
                    String service = headerMap.get(RemotingConstants.HEAD_SERVICE);
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

                    ClassLoader serviceClassLoader = ReflectCache.getServiceClassLoader(service);
                    try {
                        Thread.currentThread().setContextClassLoader(serviceClassLoader);
                        Object object = hessianInput.readObject();
                        if (object instanceof SofaRequest) {
                            final SofaRequest sofaRequest = (SofaRequest) object;
                            String[] sig = sofaRequest.getMethodArgSigs();
                            Class<?>[] classSig = new Class[sig.length];
                            generateArgTypes(sig, classSig, serviceClassLoader);

                            final Object[] args = new Object[sig.length];
                            for (int i = 0; i < sofaRequest.getMethodArgSigs().length; ++i) {
                                args[i] = hessianInput.readObject(classSig[i]);
                            }
                            sofaRequest.setMethodArgs(args);
                        }
                        requestCommand.setRequestObject(object);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }

                    input.close();
                    hessianInput.close();
                    return true;
                } catch (IOException ex) {
                    throw new DeserializationException(ex.getMessage(), ex);
                } finally {
                    recordDeserializeRequest(requestCommand);
                }
            } else if (serializer == RemotingConstants.SERIALIZE_CODE_PROTOBUF) {
                // protobuf序列化
                try {
                    String service = headerMap.get(RemotingConstants.HEAD_SERVICE);
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

                    try {
                        ClassLoader serviceClassLoader = ReflectCache.getServiceClassLoader(service);
                        Thread.currentThread().setContextClassLoader(serviceClassLoader);

                        final SofaRequest sofaRequest = new SofaRequest();
                        // 解析request信息
                        sofaRequest.setMethodName(headerMap.remove(RemotingConstants.HEAD_METHOD_NAME));
                        sofaRequest.setTargetAppName(headerMap.remove(RemotingConstants.HEAD_TARGET_APP));
                        sofaRequest.setTargetServiceUniqueName(headerMap.remove(RemotingConstants.HEAD_TARGET_SERVICE));

                        // 解析trace信息
                        Map<String, String> traceMap = new HashMap<String, String>(16);
                        ContextMapConverter.treeCopyTo(RemotingConstants.RPC_TRACE_NAME + ".", headerMap,
                            traceMap, true);
                        sofaRequest.addRequestProp(RemotingConstants.RPC_TRACE_NAME, traceMap);
                        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                            sofaRequest.addRequestProp(entry.getKey(), entry.getValue());
                        }

                        // 根据接口+方法名找到参数类型 此处要处理byte[]为空的吗
                        ProtobufSerializer protobufSerializer = ProtobufSerializer.getInstance();
                        Class requestClass = protobufSerializer.getReqClass(service,
                            sofaRequest.getMethodName(), serviceClassLoader);
                        byte[] content = requestCommand.getContent();
                        if (content == null || content.length == 0) {
                            Constructor constructor = requestClass.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            sofaRequest.setMethodArgs(new Object[] { constructor.newInstance() });
                        } else {
                            Object pbReq = protobufSerializer.decode(content, requestClass);
                            sofaRequest.setMethodArgs(new Object[] { pbReq });
                        }
                        sofaRequest.setMethodArgSigs(new String[] { requestClass.getName() });

                        requestCommand.setRequestObject(sofaRequest);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                    return true;
                } catch (DeserializationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DeserializationException(e.getMessage(), e);
                } finally {
                    recordDeserializeRequest(requestCommand);
                }
            }
        }
        return false;
    }

    /**
     * 服务端记录反序列化请求的大小和耗时
     *
     * @param requestCommand 请求对象
     */
    private void recordDeserializeRequest(RequestCommand requestCommand) {
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
            byte serializer = response.getSerializer();
            if (serializer == RemotingConstants.SERIALIZE_CODE_HESSIAN) {
                try {
                    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                    Hessian2Output output = new Hessian2Output(byteArray);
                    output.setSerializerFactory(serializerFactory);
                    output.writeObject(responseCommand.getResponseObject());
                    output.close();
                    response.setContent(byteArray.toByteArray());
                    byteArray.close();

                    return true;
                } catch (IOException ex) {
                    throw new SerializationException(ex.getMessage(), ex);
                } finally {
                    recordSerializeResponse(responseCommand);
                }
            } else if (serializer == RemotingConstants.SERIALIZE_CODE_PROTOBUF) {
                try {
                    if (responseCommand.getResponseObject() instanceof SofaResponse) {
                        SofaResponse sofaResponse = (SofaResponse) responseCommand.getResponseObject();
                        ProtobufSerializer protobufSerializer = ProtobufSerializer.getInstance();
                        if (sofaResponse.isError()) {
                            // 框架异常：错误则body序列化的是错误字符串
                            byte[] content = protobufSerializer.encode(sofaResponse.getErrorMsg());
                            response.setContent(content);
                        } else {
                            // 正确返回则解析序列化的protobuf返回对象
                            Object appResponse = sofaResponse.getAppResponse();
                            if (appResponse instanceof Throwable) {
                                // 业务异常序列化的是错误字符串
                                response.setContent(protobufSerializer.encode(((Throwable) appResponse).getMessage()));
                            } else {
                                response.setContent(protobufSerializer.encode(appResponse));
                            }
                        }
                    }
                    return true;
                } catch (SerializationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SerializationException(e.getMessage(), e);
                } finally {
                    recordSerializeResponse(responseCommand);
                }
            }
        }
        return false;
    }

    /**
     * 服务端记录序列化响应的大小和耗时
     *
     * @param responseCommand 响应体
     */
    private void recordSerializeResponse(RpcResponseCommand responseCommand) {
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
            if (serializer == RemotingConstants.SERIALIZE_CODE_HESSIAN) {
                byte[] content = responseCommand.getContent();
                if (content == null || content.length == 0) {
                    return false;
                }
                try {
                    ByteArrayInputStream input = new ByteArrayInputStream(
                        responseCommand.getContent());
                    Hessian2Input hessianInput = new Hessian2Input(input);

                    // 根据SerializeType信息决定序列化器
                    Object object;
                    boolean genericSerialize = genericSerializeResponse(invokeContext);
                    if (genericSerialize) {
                        hessianInput.setSerializerFactory(genericSerializerFactory);
                        GenericObject genericObject = (GenericObject) hessianInput.readObject();
                        SofaResponse sofaResponse = new SofaResponse();
                        sofaResponse.setErrorMsg((String) genericObject.getField("errorMsg"));
                        sofaResponse.setAppResponse(genericObject.getField("appResponse"));
                        object = sofaResponse;
                    } else {
                        hessianInput.setSerializerFactory(serializerFactory);
                        object = hessianInput.readObject();
                    }
                    responseCommand.setResponseObject(object);

                    input.close();
                    hessianInput.close();
                    return true;
                } catch (IOException ex) {
                    throw new DeserializationException(ex.getMessage(), ex);
                } finally {
                    recordDeserializeResponse(responseCommand, invokeContext);
                }
            } else if (serializer == RemotingConstants.SERIALIZE_CODE_PROTOBUF) {
                try {
                    String service = invokeContext.get(RemotingConstants.HEAD_TARGET_SERVICE);
                    String methodName = invokeContext.get(RemotingConstants.HEAD_METHOD_NAME);

                    SofaResponse sofaResponse = new SofaResponse();

                    boolean isError = false;
                    Map<String, String> header = (Map<String, String>) responseCommand.getResponseHeader();
                    if (header != null) {
                        if ("true".equals(header.get(RemotingConstants.HEAD_RESPONSE_ERROR))) {
                            header.remove(RemotingConstants.HEAD_RESPONSE_ERROR);
                            isError = true;
                        }
                        if (!header.isEmpty()) {
                            sofaResponse.setResponseProps(header);
                        }
                    }
                    ProtobufSerializer protobufSerializer = ProtobufSerializer.getInstance();
                    if (isError) {
                        String errorMessage = (String) protobufSerializer.decode(
                            responseCommand.getContent(), String.class);
                        sofaResponse.setErrorMsg(errorMessage);
                        responseCommand.setResponseObject(sofaResponse);
                    } else {
                        // 根据接口+方法名找到参数类型
                        Class responseClass = protobufSerializer.getResClass(service, methodName,
                            Thread.currentThread().getContextClassLoader());
                        byte[] content = responseCommand.getContent();
                        if (content == null || content.length == 0) {
                            Constructor constructor = responseClass.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            sofaResponse.setAppResponse(constructor.newInstance());
                        } else {
                            Object pbRes = protobufSerializer.decode(content, responseClass);
                            sofaResponse.setAppResponse(pbRes);
                        }
                        responseCommand.setResponseObject(sofaResponse);
                    }

                    return true;
                } catch (DeserializationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DeserializationException(e.getMessage(), e);
                } finally {
                    recordDeserializeResponse(responseCommand, invokeContext);
                }

            }
            return false;
        }

        return false;
    }

    /**
     * 客户端记录响应反序列化大小和响应反序列化耗时
     *
     * @param responseCommand 响应体
     */
    private void recordDeserializeResponse(RpcResponseCommand responseCommand, InvokeContext invokeContext) {
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

    protected void generateArgTypes(final String[] sig, final Class[] classSig,
                                    ClassLoader appClassLoader) throws IOException {
        for (int x = 0; x < sig.length; x++) {
            String name = ClassTypeUtils.canonicalNameToJvmName(sig[x]);
            Class<?> signature = getPrimitiveType(name);
            if (signature != null) {
                classSig[x] = signature;
            } else {
                try {
                    signature = getJdkType(name);
                } catch (ClassNotFoundException e1) {
                    signature = null;
                }
                if (signature != null) {
                    classSig[x] = signature;
                } else {
                    try {
                        classSig[x] = Class.forName(name, true, appClassLoader);
                    } catch (ClassNotFoundException e) {
                        LOGGER.error(LogCodes.getLog(LogCodes.ERROR_DECODE_REQ_SIG_CLASS_NOT_FOUND, name), e);
                        throw new IOException(LogCodes.getLog(LogCodes.ERROR_DECODE_REQ_SIG_CLASS_NOT_FOUND, name));
                    }
                }
                if (classSig[x] == null) {
                    throw new IOException(LogCodes.getLog(LogCodes.ERROR_DECODE_REQ_SIG_CLASS_NOT_FOUND, sig[x]));
                }

            }
        }
    }

    private Class<?> getPrimitiveType(String name) {
        if (null != name) {
            if ("byte".equals(name)) {
                return Byte.TYPE;
            }
            if ("short".equals(name)) {
                return Short.TYPE;
            }
            if ("int".equals(name)) {
                return Integer.TYPE;
            }
            if ("long".equals(name)) {
                return Long.TYPE;
            }
            if ("char".equals(name)) {
                return Character.TYPE;
            }
            if ("float".equals(name)) {
                return Float.TYPE;
            }
            if ("double".equals(name)) {
                return Double.TYPE;
            }
            if ("boolean".equals(name)) {
                return Boolean.TYPE;
            }
            if ("void".equals(name)) {
                return Void.TYPE;
            }
        }
        return null;
    }

    private Class<?> getJdkType(String name) throws ClassNotFoundException {
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.")) {
            return Class.forName(name);
        }
        return null;
    }

    private boolean genericSerializeResponse(InvokeContext invokeContext) {
        Integer serializeType = invokeContext == null ? null :
            (Integer) invokeContext.get(RemotingConstants.INVOKE_CTX_SERIALIZE_FACTORY_TYPE);
        return serializeType != null && serializeType == RemotingConstants.SERIALIZE_FACTORY_GENERIC;
    }

    protected boolean genericSerializeRequest(InvokeContext invokeContext) {
        Integer serializeType = invokeContext == null ? null :
            (Integer) invokeContext.get(RemotingConstants.INVOKE_CTX_SERIALIZE_FACTORY_TYPE);
        return serializeType != null && serializeType != RemotingConstants.SERIALIZE_FACTORY_NORMAL;
    }
}
