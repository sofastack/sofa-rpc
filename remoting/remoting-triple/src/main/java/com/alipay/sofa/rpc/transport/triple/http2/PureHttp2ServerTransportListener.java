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
package com.alipay.sofa.rpc.transport.triple.http2;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import com.alipay.sofa.rpc.transport.triple.http.HttpChannel;
import com.alipay.sofa.rpc.transport.triple.http.HttpInputMessage;
import com.alipay.sofa.rpc.transport.triple.http.HttpMetadata;
import com.alipay.sofa.rpc.transport.triple.http.HttpTransportListener;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import io.grpc.stub.StreamObserver;
import triple.Request;
import triple.Response;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Pure HTTP/2 server transport listener.
 * Uses native Netty HTTP/2 while maintaining gRPC protocol compatibility.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Parses gRPC frame format (1 byte compression + 4 bytes length + data)</li>
 *   <li>Handles gRPC-specific headers (grpc-timeout, etc.)</li>
 *   <li>Supports both unary and streaming calls</li>
 *   <li>Uses SofaResponse for response handling</li>
 * </ul>
 */
public class PureHttp2ServerTransportListener implements HttpTransportListener<HttpMetadata, HttpInputMessage> {

    private static final Logger          LOGGER            = LoggerFactory
                                                               .getLogger(PureHttp2ServerTransportListener.class);

    private static final String          GRPC_CONTENT_TYPE = "application/grpc";

    protected final HttpChannel          httpChannel;
    protected final ServerConfig         serverConfig;
    protected final UniqueIdInvoker      invoker;
    protected Executor                   executor;
    protected HttpMetadata               httpMetadata;
    protected HttpMessageListener        messageListener;
    protected SofaRequest                currentRequest;
    protected String                     callType;
    protected Method                     targetMethod;
    protected Serializer                 serializer;
    protected String                     serializeType;
    protected SofaStreamObserver<Object> bidiRequestObserver;
    protected StreamObserver<Object>     protoBidiRequestObserver;
    protected byte[]                     pendingBidiData;
    protected boolean                    streamComplete;
    protected boolean                    isProtoService;

    public PureHttp2ServerTransportListener(HttpChannel httpChannel, ServerConfig serverConfig,
                                            UniqueIdInvoker invoker) {
        this.httpChannel = httpChannel;
        this.serverConfig = serverConfig;
        this.invoker = invoker;
    }

    /**
     * Constructor with executor.
     *
     * @param httpChannel HTTP channel
     * @param serverConfig server configuration
     * @param invoker invoker
     * @param executor executor for request processing
     */
    public PureHttp2ServerTransportListener(HttpChannel httpChannel, ServerConfig serverConfig,
                                            UniqueIdInvoker invoker, Executor executor) {
        this.httpChannel = httpChannel;
        this.serverConfig = serverConfig;
        this.invoker = invoker;
        this.executor = executor;
    }

    @Override
    public void onMetadata(HttpMetadata metadata) {
        this.httpMetadata = metadata;

        // Use the provided executor or create one if not set
        if (executor == null) {
            executor = Executors.newCachedThreadPool();
        }

        // Build message listener for processing request data
        messageListener = buildMessageListener();
    }

    @Override
    public void onData(HttpInputMessage message) {
        if (executor == null || messageListener == null) {
            return;
        }

        executor.execute(() -> {
            try {
                byte[] body = message.getBody();
                messageListener.onMessage(body);
            } catch (Throwable t) {
                onError(t);
            } finally {
                try {
                    message.close();
                } catch (Exception e) {
                    onError(e);
                }
            }
        });
    }

    @Override
    public void onComplete() {
        // Request complete, finalize processing
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request complete for: {}", httpMetadata != null ? httpMetadata.path() : "unknown");
        }

        // Mark stream as complete
        streamComplete = true;

        // For bidirectional streaming, complete the request observer if already set
        if (protoBidiRequestObserver != null) {
            protoBidiRequestObserver.onCompleted();
        } else if (bidiRequestObserver != null) {
            bidiRequestObserver.onCompleted();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Error processing request", throwable);
        writeError(throwable);
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_2;
    }

    /**
     * Build message listener for processing request data.
     *
     * @return message listener
     */
    protected HttpMessageListener buildMessageListener() {
        SofaRequest request = buildSofaRequest(httpMetadata);
        this.currentRequest = request;

        // Check if this is a protobuf service (BindableService)
        this.isProtoService = invoker.isProtoService();

        return data -> {
            // For bidirectional streaming with existing observer, process all gRPC frames
            if (bidiRequestObserver != null || protoBidiRequestObserver != null) {
                // Data contains one or more gRPC frames, decode all and process
                List<byte[]> payloads = decodeAllGrpcFrames(data);
                LOGGER.info("Processing {} bidi frames from data", payloads.size());
                for (byte[] payload : payloads) {
                    processBidiStreamingMessage(payload);
                }
                return;
            }

            // Store raw data for potential bidi streaming processing
            pendingBidiData = data;

            // Decode gRPC framed data
            byte[] decodedData = decodeGrpcFrame(data);

            if (isProtoService) {
                // For protobuf services, the request body is the actual protobuf message
                // No Triple Request wrapper
                processProtoRequest(request, decodedData);
            } else {
                // For POJO services, parse the Triple Request wrapper
                Request tripleRequest = parseRequest(decodedData);
                if (tripleRequest == null) {
                    onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Failed to parse request"));
                    return;
                }

                // Initialize serializer
                this.serializeType = tripleRequest.getSerializeType();
                this.serializer = SerializerFactory.getSerializer(serializeType);

                // Process the request
                processRequest(request, tripleRequest);
            }
        };
    }

    /**
     * Process a bidirectional streaming message.
     *
     * @param decodedData decoded message data
     */
    @SuppressWarnings("unchecked")
    protected void processBidiStreamingMessage(byte[] decodedData) {
        try {
            if (isProtoService) {
                // For protobuf services, use protoBidiRequestObserver
                if (protoBidiRequestObserver == null) {
                    LOGGER.warn("protoBidiRequestObserver is null, cannot process message");
                    return;
                }
                // The message is raw protobuf
                // For bidirectional streaming, the request type is the type parameter of the returned StreamObserver
                // Method signature: StreamObserver<RequestType> method(StreamObserver<ResponseType> responseObserver)
                Class<?> requestType = getBidiStreamingRequestType();
                LOGGER.info("Processing bidi message, requestType: {}, data length: {}",
                    requestType != null ? requestType.getName() : "null", decodedData.length);
                if (requestType != null) {
                    Message protoMessage = parseProtoMessage(decodedData, requestType);
                    LOGGER.info("Parsed proto message: {}", protoMessage != null ? protoMessage.getClass().getName()
                        : "null");
                    if (protoMessage != null) {
                        protoBidiRequestObserver.onNext(protoMessage);
                    }
                }
            } else {
                // For POJO services, use bidiRequestObserver
                if (bidiRequestObserver == null) {
                    return;
                }
                // Parse the Triple Request wrapper
                Request tripleRequest = parseRequest(decodedData);
                if (tripleRequest == null || tripleRequest.getArgsCount() == 0) {
                    return;
                }

                // Get the argument type from the first arg type
                String typeName = tripleRequest.getArgTypesList().get(0);
                Class<?> argType = ClassTypeUtils.getClass(typeName);

                // Deserialize the argument
                byte[] argData = tripleRequest.getArgs(0).toByteArray();
                Object arg = serializer.decode(new ByteArrayWrapperByteBuf(argData), argType, null);

                // Send to the request observer
                bidiRequestObserver.onNext(arg);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process bidi streaming message", e);
            if (isProtoService && protoBidiRequestObserver != null) {
                protoBidiRequestObserver.onError(e);
            } else if (bidiRequestObserver != null) {
                bidiRequestObserver.onError(e);
            }
        }
    }

    /**
     * Parse Request protobuf from bytes.
     *
     * @param data request bytes
     * @return Request protobuf
     */
    protected Request parseRequest(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return Request.parseFrom(data);
        } catch (Exception e) {
            LOGGER.error("Failed to parse Request protobuf", e);
            return null;
        }
    }

    /**
     * Build SofaRequest from HTTP metadata.
     *
     * @param metadata HTTP metadata
     * @return SofaRequest
     */
    protected SofaRequest buildSofaRequest(HttpMetadata metadata) {
        SofaRequest request = new SofaRequest();
        String path = metadata.path();

        // Parse service and method from path: /service/method
        String serviceName = extractServiceName(path);
        String methodName = extractMethodName(path);

        request.setInterfaceName(serviceName);
        request.setMethodName(methodName);

        // Parse headers
        parseHeaders(request, metadata.headers());

        return request;
    }

    /**
     * Decode gRPC framed data.
     * gRPC frame format: 1 byte compression flag + 4 byte length + data
     *
     * @param data framed data
     * @return decoded data
     */
    protected byte[] decodeGrpcFrame(byte[] data) {
        if (data == null || data.length < 5) {
            return data;
        }

        // Check compression flag (first byte)
        // Currently only identity (no compression) is supported

        // Read length (4 bytes, big-endian)
        int length = ((data[1] & 0xFF) << 24) |
            ((data[2] & 0xFF) << 16) |
            ((data[3] & 0xFF) << 8) |
            (data[4] & 0xFF);

        // Extract payload
        if (data.length >= 5 + length) {
            byte[] payload = new byte[length];
            System.arraycopy(data, 5, payload, 0, length);
            return payload;
        }

        return data;
    }

    /**
     * Decode all gRPC frames from data.
     * Multiple gRPC frames can be concatenated in a single HTTP/2 data frame.
     *
     * @param data framed data containing one or more gRPC frames
     * @return list of decoded payloads
     */
    protected List<byte[]> decodeAllGrpcFrames(byte[] data) {
        List<byte[]> payloads = new ArrayList<>();
        if (data == null || data.length < 5) {
            LOGGER.info("decodeAllGrpcFrames: data is null or too short, length: {}",
                data != null ? data.length : -1);
            return payloads;
        }

        LOGGER.info("decodeAllGrpcFrames: data length: {}, first 5 bytes: {} {} {} {} {}",
            data.length,
            data.length > 0 ? data[0] : -1,
            data.length > 1 ? data[1] : -1,
            data.length > 2 ? data[2] : -1,
            data.length > 3 ? data[3] : -1,
            data.length > 4 ? data[4] : -1);

        int offset = 0;
        while (offset + 5 <= data.length) {
            // Read length (4 bytes, big-endian) after compression flag
            int length = ((data[offset + 1] & 0xFF) << 24) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 8) |
                (data[offset + 4] & 0xFF);

            LOGGER.info("decodeAllGrpcFrames: offset: {}, length: {}, data.length: {}",
                offset, length, data.length);

            if (offset + 5 + length > data.length) {
                LOGGER.info("decodeAllGrpcFrames: breaking, offset + 5 + length > data.length");
                break;
            }

            // Extract payload
            byte[] payload = new byte[length];
            System.arraycopy(data, offset + 5, payload, 0, length);
            payloads.add(payload);

            offset += 5 + length;
        }

        LOGGER.info("decodeAllGrpcFrames: returning {} payloads", payloads.size());
        return payloads;
    }

    /**
     * Process the request.
     *
     * @param request SofaRequest
     * @param tripleRequest Triple Request protobuf
     */
    protected void processRequest(SofaRequest request, Request tripleRequest) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Get service class loader
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(request);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);

            // Try to find method - first try unary, then server streaming, then bidirectional streaming
            Method declaredMethod = invoker.getDeclaredMethod(request, tripleRequest, RpcConstants.INVOKER_TYPE_UNARY);
            if (declaredMethod == null) {
                // Try server streaming
                declaredMethod = invoker.getDeclaredMethod(request, tripleRequest,
                    RpcConstants.INVOKER_TYPE_SERVER_STREAMING);
                if (declaredMethod != null) {
                    callType = RpcConstants.INVOKER_TYPE_SERVER_STREAMING;
                } else {
                    // Try bidirectional streaming
                    declaredMethod = invoker.getDeclaredMethod(request, tripleRequest,
                        RpcConstants.INVOKER_TYPE_BI_STREAMING);
                    if (declaredMethod != null) {
                        callType = RpcConstants.INVOKER_TYPE_BI_STREAMING;
                    }
                }
            } else {
                // Check if it's actually a streaming method based on signature
                callType = determineCallType(declaredMethod);
            }

            if (declaredMethod == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER,
                    "Cannot find invoke method " + request.getMethodName()));
                return;
            }

            this.targetMethod = declaredMethod;

            // Set request parameters
            setRequestParams(request, tripleRequest, declaredMethod);

            // For streaming calls, handle differently
            if (RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType)) {
                // For bidirectional streaming, set response observer and invoke
                SofaStreamObserver<Object> responseObserver = createServerStreamObserver(tripleRequest);
                request.getMethodArgs()[request.getMethodArgs().length - 1] = responseObserver;
                SofaResponse response = invoker.invoke(request);
                handleBidiStreaming(request, response, tripleRequest);
            } else if (RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType)) {
                // For server streaming, set response observer and invoke
                SofaStreamObserver<Object> streamObserver = createServerStreamObserver(tripleRequest);
                request.getMethodArgs()[request.getMethodArgs().length - 1] = streamObserver;
                invoker.invoke(request);
            } else {
                // Unary call
                SofaResponse response = invoker.invoke(request);
                handleUnaryResponse(response, declaredMethod, tripleRequest);
            }

        } catch (Exception e) {
            LOGGER.error("Service invocation failed: " + request.getInterfaceName() + "." + request.getMethodName(), e);
            onError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Process a protobuf service request.
     * For protobuf services (BindableService), the request body is the actual protobuf message,
     * not wrapped in a Triple Request protobuf.
     *
     * @param request SofaRequest
     * @param data raw protobuf message data
     */
    protected void processProtoRequest(SofaRequest request, byte[] data) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing protobuf service request: {}.{}, data length: {}",
                request.getInterfaceName(), request.getMethodName(), data != null ? data.length : 0);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Get service class loader
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(request);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);

            // Get the provider config to access the service implementation
            ProviderConfig providerConfig = invoker.getProviderConfig();
            if (providerConfig == null || providerConfig.getRef() == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER,
                    "Cannot find provider config"));
                return;
            }

            Object serviceInstance = providerConfig.getRef();

            // Find the method on the service implementation
            Method declaredMethod = findProtoMethod(serviceInstance.getClass(), request.getMethodName());
            if (declaredMethod == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER,
                    "Cannot find method " + request.getMethodName()));
                return;
            }

            this.targetMethod = declaredMethod;
            this.callType = determineCallType(declaredMethod);

            // Get the request message type from method parameters
            Class<?>[] paramTypes = declaredMethod.getParameterTypes();
            if (paramTypes.length == 0) {
                onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE,
                    "Method has no parameters"));
                return;
            }

            // For bidirectional streaming, we don't parse the first data frame here
            // The request type comes from the return type StreamObserver<RequestType>
            // and data will be processed through the returned observer
            if (RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType)) {
                // Note: 'data' is already decoded (single protobuf message)
                // But pendingBidiData contains the original gRPC framed data with potentially multiple messages
                final byte[] rawGrpcData = pendingBidiData;

                // Prepare method arguments - just the response observer
                Object[] invokeArgs = new Object[1];
                StreamObserver<Object> responseObserver = createProtoStreamObserver();
                invokeArgs[0] = responseObserver;

                request.setMethod(declaredMethod);
                request.setMethodArgs(invokeArgs);
                request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(paramTypes, true));

                // Invoke the method and get the request observer
                @SuppressWarnings("unchecked")
                StreamObserver<Object> requestObserver = (StreamObserver<Object>) declaredMethod.invoke(
                    serviceInstance, invokeArgs);
                this.protoBidiRequestObserver = requestObserver;

                // Process all gRPC frames from the raw data
                if (rawGrpcData != null && rawGrpcData.length > 0) {
                    List<byte[]> payloads = decodeAllGrpcFrames(rawGrpcData);
                    LOGGER.info("Processing {} bidi frames from pending data", payloads.size());
                    for (byte[] payload : payloads) {
                        processBidiStreamingMessage(payload);
                    }
                    pendingBidiData = null;
                }

                // If stream was already marked complete, complete the request observer
                if (streamComplete && protoBidiRequestObserver != null) {
                    protoBidiRequestObserver.onCompleted();
                }

                return;
            }

            // Parse the protobuf request message (for unary and server streaming)
            Message protoRequest = parseProtoMessage(data, paramTypes[0]);
            if (protoRequest == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE,
                    "Failed to parse protobuf request"));
                return;
            }

            // Prepare method arguments (only for unary and server streaming)
            Object[] invokeArgs;
            if (RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType)) {
                // For server streaming, the method takes (request, StreamObserver)
                invokeArgs = new Object[2];
                invokeArgs[0] = protoRequest;
                StreamObserver<Object> responseObserver = createProtoStreamObserver();
                invokeArgs[1] = responseObserver;

                request.setMethod(declaredMethod);
                request.setMethodArgs(invokeArgs);
                request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(paramTypes, true));

                // Invoke the method
                declaredMethod.invoke(serviceInstance, invokeArgs);
            } else {
                // For unary call, the method takes (request, StreamObserver)
                invokeArgs = new Object[2];
                invokeArgs[0] = protoRequest;
                StreamObserver<Object> responseObserver = createProtoStreamObserver();
                invokeArgs[1] = responseObserver;

                request.setMethod(declaredMethod);
                request.setMethodArgs(invokeArgs);
                request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(paramTypes, true));

                // Invoke the method
                declaredMethod.invoke(serviceInstance, invokeArgs);
            }

        } catch (Exception e) {
            LOGGER.error(
                "Protobuf service invocation failed: " + request.getInterfaceName() + "." + request.getMethodName(), e);
            onError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Find method on the service implementation class.
     *
     * @param serviceClass service implementation class
     * @param methodName method name
     * @return method or null if not found
     */
    protected Method findProtoMethod(Class<?> serviceClass, String methodName) {
        // First try exact match
        for (Method method : serviceClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        // Try case-insensitive match (gRPC method names might have different casing)
        String lowerMethodName = methodName.toLowerCase();
        for (Method method : serviceClass.getMethods()) {
            if (method.getName().toLowerCase().equals(lowerMethodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Parse protobuf message from bytes.
     *
     * @param data message bytes
     * @param messageType message type
     * @return parsed message or null
     */
    protected Message parseProtoMessage(byte[] data, Class<?> messageType) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            // Use the parseFrom method to parse the message
            Method parseFrom = messageType.getMethod("parseFrom", byte[].class);
            return (Message) parseFrom.invoke(null, data);
        } catch (Exception e) {
            LOGGER.error("Failed to parse protobuf message of type: " + messageType.getName(), e);
            return null;
        }
    }

    /**
     * Create a StreamObserver for protobuf service responses.
     *
     * @return StreamObserver
     */
    protected StreamObserver<Object> createProtoStreamObserver() {
        return new StreamObserver<Object>() {
            @Override
            public void onNext(Object message) {
                // Write protobuf response directly (not wrapped in Triple Response)
                if (message instanceof Message) {
                    byte[] data = ((Message) message).toByteArray();
                    byte[] grpcFrame = encodeGrpcFrame(data);

                    if (httpChannel != null && httpChannel.isActive()) {
                        Http2Channel http2Channel = (Http2Channel) httpChannel;
                        http2Channel.writeResponse(grpcFrame);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                writeError(throwable);
            }

            @Override
            public void onCompleted() {
                // Write gRPC trailers to complete the stream
                writeGrpcTrailers(0, null);
            }
        };
    }

    /**
     * Determine call type from method signature.
     *
     * @param method target method
     * @return call type
     */
    protected String determineCallType(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            Class<?> lastParamType = paramTypes[paramTypes.length - 1];
            if (SofaStreamObserver.class.isAssignableFrom(lastParamType) ||
                StreamObserver.class.isAssignableFrom(lastParamType)) {
                // Check if it's bidirectional/client streaming (return type is StreamObserver)
                // Both bidirectional and client streaming return StreamObserver<RequestType>
                if (SofaStreamObserver.class.isAssignableFrom(method.getReturnType()) ||
                    StreamObserver.class.isAssignableFrom(method.getReturnType())) {
                    return RpcConstants.INVOKER_TYPE_BI_STREAMING;
                }
                return RpcConstants.INVOKER_TYPE_SERVER_STREAMING;
            }
        }
        return RpcConstants.INVOKER_TYPE_UNARY;
    }

    /**
     * Get the request type for bidirectional streaming.
     * For bidirectional streaming, the method returns StreamObserver&lt;RequestType&gt;,
     * so we need to extract the type parameter from the return type.
     *
     * @return request type class, or null if cannot determine
     */
    protected Class<?> getBidiStreamingRequestType() {
        if (targetMethod == null) {
            return null;
        }

        // For bidirectional streaming, the return type is StreamObserver<RequestType>
        java.lang.reflect.Type genericReturnType = targetMethod.getGenericReturnType();
        if (genericReturnType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericReturnType;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return null;
    }

    /**
     * Set request parameters from triple request.
     *
     * @param request SofaRequest
     * @param tripleRequest Triple Request protobuf
     * @param declaredMethod target method
     */
    protected void setRequestParams(SofaRequest request, Request tripleRequest, Method declaredMethod) {
        Class<?>[] argTypes = getArgTypes(tripleRequest, callType);
        Object[] invokeArgs;

        // For bidirectional streaming, the method takes only SofaStreamObserver as parameter
        // No arguments to deserialize from the request
        if (RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType)) {
            invokeArgs = new Object[1]; // Just the observer, will be set later
        } else {
            invokeArgs = getInvokeArgs(tripleRequest, argTypes);

            // For server streaming, add StreamObserver as last argument
            if (RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType)) {
                Object[] newArgs = new Object[invokeArgs.length + 1];
                System.arraycopy(invokeArgs, 0, newArgs, 0, invokeArgs.length);
                // StreamObserver will be set later
                invokeArgs = newArgs;
            }
        }

        request.setMethod(declaredMethod);
        request.setMethodArgs(invokeArgs);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
    }

    /**
     * Get argument types from request.
     *
     * @param request triple request
     * @param callType call type
     * @return argument types
     */
    protected Class<?>[] getArgTypes(Request request, String callType) {
        ProtocolStringList argTypesList = request.getArgTypesList();
        boolean isStreaming = RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType) ||
            RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType);
        int size = isStreaming ? argTypesList.size() + 1 : argTypesList.size();
        Class<?>[] argTypes = new Class[size];

        for (int i = 0; i < argTypesList.size(); i++) {
            String typeName = argTypesList.get(i);
            argTypes[i] = ClassTypeUtils.getClass(typeName);
        }

        if (isStreaming) {
            argTypes[size - 1] = SofaStreamObserver.class;
        }
        return argTypes;
    }

    /**
     * Get arguments from request.
     *
     * @param request triple request
     * @param argTypes argument types
     * @return arguments
     */
    protected Object[] getInvokeArgs(Request request, Class<?>[] argTypes) {
        List<ByteString> argsList = request.getArgsList();
        Object[] args = new Object[argsList.size()];

        for (int i = 0; i < argsList.size(); i++) {
            byte[] data = argsList.get(i).toByteArray();
            args[i] = serializer.decode(new ByteArrayWrapperByteBuf(data), argTypes[i], null);
        }
        return args;
    }

    /**
     * Handle response based on call type.
     *
     * @param request SofaRequest
     * @param response SofaResponse
     * @param declaredMethod target method
     * @param tripleRequest triple request
     */
    protected void handleResponse(SofaRequest request, SofaResponse response, Method declaredMethod,
                                  Request tripleRequest) {
        if (RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType)) {
            // Server streaming - create StreamObserver for response
            SofaStreamObserver<Object> streamObserver = createServerStreamObserver(tripleRequest);
            request.getMethodArgs()[request.getMethodArgs().length - 1] = streamObserver;
            // Re-invoke with stream observer
            invoker.invoke(request);
        } else if (RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType)) {
            // Bidirectional streaming - set response observer and invoke
            SofaStreamObserver<Object> responseObserver = createServerStreamObserver(tripleRequest);
            request.getMethodArgs()[request.getMethodArgs().length - 1] = responseObserver;
            // Invoke to get the request observer
            SofaResponse bidiResponse = invoker.invoke(request);
            handleBidiStreaming(request, bidiResponse, tripleRequest);
        } else {
            // Unary call
            handleUnaryResponse(response, declaredMethod, tripleRequest);
        }
    }

    /**
     * Handle unary response.
     *
     * @param response SofaResponse
     * @param declaredMethod target method
     * @param tripleRequest triple request
     */
    protected void handleUnaryResponse(SofaResponse response, Method declaredMethod, Request tripleRequest) {
        if (response == null) {
            writeError(new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, "Response is null"));
            return;
        }

        if (response.isError()) {
            writeError(new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg()));
            return;
        }

        Object ret = response.getAppResponse();
        if (ret instanceof Throwable) {
            writeError((Throwable) ret);
            return;
        }

        if (ret == null) {
            ret = ClassUtils.getDefaultPrimitiveValue(declaredMethod.getReturnType());
        }

        // Encode response
        Response tripleResponse = buildResponse(ret, declaredMethod.getReturnType(), tripleRequest.getSerializeType());

        // Write response with end of stream flag to send trailers
        writeResponse(tripleResponse, true);
    }

    /**
     * Build Response protobuf from object.
     *
     * @param result result object
     * @param returnType return type
     * @param serializeType serialize type
     * @return Response protobuf
     */
    protected Response buildResponse(Object result, Class<?> returnType, String serializeType) {
        Response.Builder builder = Response.newBuilder();
        builder.setSerializeType(serializeType);
        builder.setType(returnType.getName());
        if (result != null) {
            builder.setData(ByteString.copyFrom(serializer.encode(result, null).array()));
        }
        return builder.build();
    }

    /**
     * Create server stream observer for server streaming calls.
     *
     * @param tripleRequest triple request
     * @return SofaStreamObserver
     */
    protected SofaStreamObserver<Object> createServerStreamObserver(Request tripleRequest) {
        return new SofaStreamObserver<Object>() {
            @Override
            public void onNext(Object message) {
                Response response = buildResponse(message, message.getClass(), serializeType);
                writeResponse(response);
            }

            @Override
            public void onCompleted() {
                writeGrpcTrailers(0, null);
            }

            @Override
            public void onError(Throwable throwable) {
                writeError(throwable);
            }
        };
    }

    /**
     * Handle bidirectional streaming.
     *
     * @param request SofaRequest
     * @param response SofaResponse
     * @param tripleRequest triple request
     */
    @SuppressWarnings("unchecked")
    protected void handleBidiStreaming(SofaRequest request, SofaResponse response, Request tripleRequest) {
        // For bidirectional streaming, the response is a SofaStreamObserver
        // that will receive messages from the client
        if (response == null || response.getAppResponse() == null) {
            writeError(new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, "Bidi streaming response is null"));
            return;
        }

        // The appResponse is a SofaStreamObserver<ClientRequest> that receives client messages
        // Store it for processing subsequent data frames
        bidiRequestObserver = (SofaStreamObserver<Object>) response.getAppResponse();

        // Process all pending data (the first data frame may contain multiple gRPC messages)
        if (pendingBidiData != null) {
            List<byte[]> payloads = decodeAllGrpcFrames(pendingBidiData);
            for (byte[] payload : payloads) {
                processBidiStreamingMessage(payload);
            }
            pendingBidiData = null;
        }

        // If stream was already marked complete, complete the request observer
        if (streamComplete) {
            bidiRequestObserver.onCompleted();
        }
    }

    /**
     * Write response to client.
     *
     * @param response Response protobuf
     */
    protected void writeResponse(Response response) {
        writeResponse(response, false);
    }

    /**
     * Write response to client.
     *
     * @param response Response protobuf
     * @param endOfStream whether this is the end of stream
     */
    protected void writeResponse(Response response, boolean endOfStream) {
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            byte[] data = response.toByteArray();
            byte[] grpcFrame = encodeGrpcFrame(data);

            Http2Channel http2Channel = (Http2Channel) httpChannel;
            if (endOfStream) {
                http2Channel.writeResponseAndComplete(grpcFrame);
            } else {
                http2Channel.writeResponse(grpcFrame);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write response", e);
            onError(e);
        }
    }

    /**
     * Write gRPC trailers.
     *
     * @param status gRPC status code
     * @param message error message
     */
    protected void writeGrpcTrailers(int status, String message) {
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            Http2Channel http2Channel = (Http2Channel) httpChannel;
            http2Channel.writeGrpcTrailers(status, message);
        } catch (Exception e) {
            LOGGER.error("Failed to write trailers", e);
        }
    }

    /**
     * Write error response.
     *
     * @param throwable error
     */
    protected void writeError(Throwable throwable) {
        LOGGER.error("Writing error response", throwable);
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            Http2Channel http2Channel = (Http2Channel) httpChannel;
            // Map exception to gRPC status code
            io.grpc.Status status = io.grpc.Status.fromThrowable(throwable);
            http2Channel.writeGrpcTrailers(status.getCode().value(), status.getDescription());
        } catch (Exception e) {
            LOGGER.error("Failed to write error response", e);
            httpChannel.close();
        }
    }

    /**
     * Encode data in gRPC frame format.
     * gRPC frame format: 1 byte compression flag + 4 byte length + data
     *
     * @param data the data to encode
     * @return gRPC framed data
     */
    protected byte[] encodeGrpcFrame(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        // gRPC frame: 1 byte compression (0 = no compression) + 4 bytes length + data
        byte[] frame = new byte[5 + data.length];
        frame[0] = 0; // No compression

        // Write length as 4 bytes big-endian
        frame[1] = (byte) ((data.length >> 24) & 0xFF);
        frame[2] = (byte) ((data.length >> 16) & 0xFF);
        frame[3] = (byte) ((data.length >> 8) & 0xFF);
        frame[4] = (byte) (data.length & 0xFF);

        // Copy data
        System.arraycopy(data, 0, frame, 5, data.length);

        return frame;
    }

    /**
     * Extract service name from path.
     *
     * @param path request path
     * @return service name
     */
    protected String extractServiceName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }
        return path;
    }

    /**
     * Extract method name from path.
     *
     * @param path request path
     * @return method name
     */
    protected String extractMethodName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0 && slashIndex < path.length() - 1) {
            return path.substring(slashIndex + 1);
        }
        return "";
    }

    /**
     * Parse headers into SofaRequest.
     *
     * @param request SofaRequest
     * @param headers HTTP headers
     */
    protected void parseHeaders(SofaRequest request, com.alipay.sofa.rpc.transport.triple.http.HttpHeaders headers) {
        // Parse gRPC metadata
        String timeoutStr = headers != null ? headers.get("grpc-timeout") : null;
        if (timeoutStr != null) {
            // Parse gRPC timeout format (e.g., "10S", "100m")
            request.setTimeout(parseGrpcTimeout(timeoutStr));
        }

        // Parse custom triple headers
        String serialization = headers != null ? headers.get("tri-serialize-type") : null;
        if (serialization != null) {
            try {
                request.setSerializeType(Byte.parseByte(serialization));
            } catch (NumberFormatException e) {
                // Ignore invalid serialization type
            }
        }
    }

    /**
     * Parse gRPC timeout string.
     *
     * @param timeoutStr timeout string (e.g., "10S", "100m")
     * @return timeout in milliseconds
     */
    protected int parseGrpcTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.isEmpty()) {
            return 0;
        }

        try {
            // Extract unit and value
            char unit = timeoutStr.charAt(timeoutStr.length() - 1);
            long value = Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1));

            switch (unit) {
                case 'H':
                    return (int) (value * 60 * 60 * 1000);
                case 'M':
                    return (int) (value * 60 * 1000);
                case 'S':
                    return (int) (value * 1000);
                case 'm':
                    return (int) value;
                case 'u':
                    return (int) (value / 1000);
                case 'n':
                    return (int) (value / 1000000);
                default:
                    return (int) value;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if this listener supports the given content type.
     *
     * @param contentType content type
     * @return true if supported
     */
    public boolean supportsContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(GRPC_CONTENT_TYPE) ||
            contentType.startsWith("application/json");
    }

    /**
     * HTTP message listener interface.
     */
    @FunctionalInterface
    protected interface HttpMessageListener {
        void onMessage(byte[] data);
    }
}