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
package com.alipay.sofa.rpc.transport.triple.http3;

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
import com.alipay.sofa.rpc.transport.triple.http.*;
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
 * HTTP/3 server transport listener implementation.
 * HTTP/3 uses QUIC transport and supports all four streaming modes.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Parses gRPC frame format (1 byte compression + 4 bytes length + data)</li>
 *   <li>Handles gRPC-specific headers (grpc-timeout, etc.)</li>
 *   <li>Supports both unary and streaming calls</li>
 *   <li>Supports both protobuf services and POJO services</li>
 * </ul>
 *
 * <h3>Architecture:</h3>
 * <pre>
 *  +------------------+
 *  | onMetadata()     | -> Build message listener
 *  +------------------+
 *           |
 *           v
 *  +------------------+
 *  | onData()         | -> Process request data
 *  +------------------+
 *           |
 *           v
 *  +------------------+     +----------------------+
 *  | processRequest() | --> | processProtoRequest()| (Protobuf service)
 *  +------------------+     +----------------------+
 *           |                        |
 *           v                        v
 *  +------------------+     +----------------------+
 *  | handleResponse() | <-- | createStreamObserver |
 *  +------------------+     +----------------------+
 * </pre>
 *
 * Note: This implementation requires QUIC transport support.
 */
public class Http3ServerTransportListener implements HttpTransportListener<Http3Metadata, Http3InputMessage> {

    // ==================== Constants ====================

    private static final Logger          LOGGER                 = LoggerFactory
                                                                    .getLogger(Http3ServerTransportListener.class);

    /** gRPC content type */
    private static final String          GRPC_CONTENT_TYPE      = "application/grpc";

    /** JSON content type prefix */
    private static final String          JSON_CONTENT_TYPE      = "application/json";

    /** gRPC frame header size: 1 byte compression flag + 4 bytes length */
    private static final int             GRPC_FRAME_HEADER_SIZE = 5;

    /** HTTP status OK */
    private static final String          HTTP_STATUS_OK         = "200";

    /** gRPC status header */
    private static final String          GRPC_STATUS_HEADER     = "grpc-status";

    /** gRPC message header */
    private static final String          GRPC_MESSAGE_HEADER    = "grpc-message";

    /** Content-Type header */
    private static final String          CONTENT_TYPE_HEADER    = "content-type";

    // ==================== Fields ====================

    /** HTTP channel for writing responses */
    protected final HttpChannel          httpChannel;

    /** Server configuration */
    protected final ServerConfig         serverConfig;

    /** Invoker for service method calls */
    protected final UniqueIdInvoker      invoker;

    /** Executor for async request processing */
    protected Executor                   executor;

    /** Current HTTP metadata */
    protected Http3Metadata              httpMetadata;

    /** Message listener for processing request data */
    protected HttpMessageListener        messageListener;

    /** Current SofaRequest being processed */
    protected SofaRequest                currentRequest;

    /** Call type: UNARY, SERVER_STREAMING, or BI_STREAMING */
    protected String                     callType;

    /** Target method to invoke */
    protected Method                     targetMethod;

    /** Serializer for POJO services */
    protected Serializer                 serializer;

    /** Serialization type */
    protected String                     serializeType;

    /** Request observer for bidirectional streaming (POJO) */
    protected SofaStreamObserver<Object> bidiRequestObserver;

    /** Request observer for bidirectional streaming (Protobuf) */
    protected StreamObserver<Object>     protoBidiRequestObserver;

    /** Pending data for bidirectional streaming */
    protected byte[]                     pendingBidiData;

    /** Whether the stream is complete */
    protected boolean                    streamComplete;

    /** Whether this is a protobuf service */
    protected boolean                    isProtoService;

    /** Whether headers have been sent */
    protected boolean                    headersSent;

    // ==================== Constructor ====================

    public Http3ServerTransportListener(HttpChannel channel, ServerConfig serverConfig,
                                        UniqueIdInvoker invoker) {
        this.httpChannel = channel;
        this.serverConfig = serverConfig;
        this.invoker = invoker;
    }

    // ==================== HttpTransportListener Implementation ====================

    @Override
    public void onMetadata(Http3Metadata metadata) {
        this.httpMetadata = metadata;
        this.executor = Executors.newCachedThreadPool();

        if (invoker != null) {
            this.isProtoService = invoker.isProtoService();
        }

        this.messageListener = buildMessageListener();
    }

    @Override
    public void onData(Http3InputMessage message) {
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
                safeClose(message);
            }
        });
    }

    @Override
    public void onComplete() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request complete for: {}", httpMetadata != null ? httpMetadata.path() : "unknown");
        }

        streamComplete = true;

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
        return HttpVersion.HTTP_3;
    }

    // ==================== Request Processing ====================

    /**
     * Build message listener for processing request data.
     */
    protected HttpMessageListener buildMessageListener() {
        SofaRequest request = buildSofaRequest(httpMetadata);
        this.currentRequest = request;

        return data -> {
            // For bidirectional streaming with existing observer, process all frames directly
            if (bidiRequestObserver != null || protoBidiRequestObserver != null) {
                processBidiStreamingMessages(data);
                return;
            }

            pendingBidiData = data;
            byte[] decodedData = decodeGrpcFrame(data);

            if (isProtoService) {
                processProtoRequest(request, decodedData);
            } else {
                processPojoRequest(request, decodedData);
            }
        };
    }

    /**
     * Process POJO service request.
     */
    private void processPojoRequest(SofaRequest request, byte[] decodedData) {
        Request tripleRequest = parseRequest(decodedData);
        if (tripleRequest == null) {
            onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Failed to parse request"));
            return;
        }

        this.serializeType = tripleRequest.getSerializeType();
        this.serializer = SerializerFactory.getSerializer(serializeType);
        processRequest(request, tripleRequest);
    }

    /**
     * Process bidirectional streaming messages.
     */
    private void processBidiStreamingMessages(byte[] data) {
        List<byte[]> payloads = decodeAllGrpcFrames(data);
        for (byte[] payload : payloads) {
            processBidiStreamingMessage(payload);
        }
    }

    /**
     * Process the request for POJO services.
     */
    protected void processRequest(SofaRequest request, Request tripleRequest) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(request);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);

            Method declaredMethod = findMethod(request, tripleRequest);
            if (declaredMethod == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER,
                    "Cannot find invoke method " + request.getMethodName()));
                return;
            }

            this.targetMethod = declaredMethod;
            this.callType = determineCallType(declaredMethod);
            setRequestParams(request, tripleRequest, declaredMethod);

            invokeMethod(request, tripleRequest);

        } catch (Exception e) {
            LOGGER.error("Service invocation failed: " + request.getInterfaceName() + "." + request.getMethodName(), e);
            onError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Find method by trying different call types.
     */
    private Method findMethod(SofaRequest request, Request tripleRequest) {
        Method method = invoker.getDeclaredMethod(request, tripleRequest, RpcConstants.INVOKER_TYPE_UNARY);
        if (method != null) {
            return method;
        }

        method = invoker.getDeclaredMethod(request, tripleRequest, RpcConstants.INVOKER_TYPE_SERVER_STREAMING);
        if (method != null) {
            this.callType = RpcConstants.INVOKER_TYPE_SERVER_STREAMING;
            return method;
        }

        method = invoker.getDeclaredMethod(request, tripleRequest, RpcConstants.INVOKER_TYPE_BI_STREAMING);
        if (method != null) {
            this.callType = RpcConstants.INVOKER_TYPE_BI_STREAMING;
        }
        return method;
    }

    /**
     * Invoke the method based on call type.
     */
    private void invokeMethod(SofaRequest request, Request tripleRequest) throws Exception {
        switch (callType) {
            case RpcConstants.INVOKER_TYPE_BI_STREAMING:
                invokeBidiStreaming(request, tripleRequest);
                break;
            case RpcConstants.INVOKER_TYPE_SERVER_STREAMING:
                invokeServerStreaming(request, tripleRequest);
                break;
            default:
                invokeUnary(request, tripleRequest);
                break;
        }
    }

    /**
     * Invoke unary call.
     */
    private void invokeUnary(SofaRequest request, Request tripleRequest) throws Exception {
        SofaResponse response = invoker.invoke(request);
        handleUnaryResponse(response, targetMethod, tripleRequest);
    }

    /**
     * Invoke server streaming call.
     */
    private void invokeServerStreaming(SofaRequest request, Request tripleRequest) {
        SofaStreamObserver<Object> streamObserver = createServerStreamObserver(tripleRequest);
        request.getMethodArgs()[request.getMethodArgs().length - 1] = streamObserver;
        invoker.invoke(request);
    }

    /**
     * Invoke bidirectional streaming call.
     */
    private void invokeBidiStreaming(SofaRequest request, Request tripleRequest) {
        SofaStreamObserver<Object> responseObserver = createServerStreamObserver(tripleRequest);
        request.getMethodArgs()[request.getMethodArgs().length - 1] = responseObserver;
        SofaResponse response = invoker.invoke(request);
        handleBidiStreaming(request, response, tripleRequest);
    }

    // ==================== Protobuf Service Processing ====================

    /**
     * Process a protobuf service request.
     */
    protected void processProtoRequest(SofaRequest request, byte[] data) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processing protobuf service request: {}.{}, data length: {}",
                request.getInterfaceName(), request.getMethodName(), data != null ? data.length : 0);
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(request);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);

            ProviderConfig<?> providerConfig = invoker.getProviderConfig();
            if (providerConfig == null || providerConfig.getRef() == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find provider config"));
                return;
            }

            Object serviceInstance = providerConfig.getRef();
            Method declaredMethod = findProtoMethod(serviceInstance.getClass(), request.getMethodName());
            if (declaredMethod == null) {
                onError(new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER,
                    "Cannot find method " + request.getMethodName()));
                return;
            }

            this.targetMethod = declaredMethod;
            this.callType = determineCallType(declaredMethod);

            invokeProtoMethod(request, serviceInstance, declaredMethod, data);

        } catch (Exception e) {
            LOGGER.error(
                "Protobuf service invocation failed: " + request.getInterfaceName() + "." + request.getMethodName(), e);
            onError(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Invoke protobuf service method.
     */
    private void invokeProtoMethod(SofaRequest request, Object serviceInstance, Method declaredMethod, byte[] data)
        throws Exception {
        Class<?>[] paramTypes = declaredMethod.getParameterTypes();
        if (paramTypes.length == 0) {
            onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Method has no parameters"));
            return;
        }

        Message protoRequest = parseProtoMessage(data, paramTypes[0]);
        if (protoRequest == null) {
            onError(new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "Failed to parse protobuf request"));
            return;
        }

        switch (callType) {
            case RpcConstants.INVOKER_TYPE_BI_STREAMING:
                invokeProtoBidiStreaming(request, serviceInstance, declaredMethod, paramTypes);
                break;
            default:
                invokeProtoUnaryOrServerStreaming(request, serviceInstance, declaredMethod, paramTypes, protoRequest);
                break;
        }
    }

    /**
     * Invoke protobuf bidirectional streaming.
     */
    private void invokeProtoBidiStreaming(SofaRequest request, Object serviceInstance, Method declaredMethod,
                                          Class<?>[] paramTypes) throws Exception {
        Object[] invokeArgs = new Object[1];
        StreamObserver<Object> responseObserver = createProtoStreamObserver();
        invokeArgs[0] = responseObserver;

        request.setMethod(declaredMethod);
        request.setMethodArgs(invokeArgs);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(paramTypes, true));

        @SuppressWarnings("unchecked")
        StreamObserver<Object> requestObserver = (StreamObserver<Object>) declaredMethod.invoke(serviceInstance,
            invokeArgs);
        this.protoBidiRequestObserver = requestObserver;

        processPendingBidiData();
        completeStreamIfNeeded();
    }

    /**
     * Invoke protobuf unary or server streaming.
     */
    private void invokeProtoUnaryOrServerStreaming(SofaRequest request, Object serviceInstance, Method declaredMethod,
                                                   Class<?>[] paramTypes, Message protoRequest) throws Exception {
        Object[] invokeArgs = new Object[2];
        invokeArgs[0] = protoRequest;
        invokeArgs[1] = createProtoStreamObserver();

        request.setMethod(declaredMethod);
        request.setMethodArgs(invokeArgs);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(paramTypes, true));

        declaredMethod.invoke(serviceInstance, invokeArgs);
    }

    /**
     * Process pending bidirectional streaming data.
     */
    private void processPendingBidiData() {
        if (pendingBidiData != null) {
            List<byte[]> payloads = decodeAllGrpcFrames(pendingBidiData);
            for (byte[] payload : payloads) {
                processBidiStreamingMessage(payload);
            }
            pendingBidiData = null;
        }
    }

    /**
     * Complete stream if already marked complete.
     */
    private void completeStreamIfNeeded() {
        if (streamComplete && protoBidiRequestObserver != null) {
            protoBidiRequestObserver.onCompleted();
        }
    }

    // ==================== Bidirectional Streaming ====================

    /**
     * Process a bidirectional streaming message.
     */
    @SuppressWarnings("unchecked")
    protected void processBidiStreamingMessage(byte[] decodedData) {
        try {
            if (isProtoService) {
                processProtoBidiMessage(decodedData);
            } else {
                processPojoBidiMessage(decodedData);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process bidi streaming message", e);
            notifyBidiError(e);
        }
    }

    /**
     * Process protobuf bidirectional streaming message.
     */
    private void processProtoBidiMessage(byte[] decodedData) {
        if (protoBidiRequestObserver == null || targetMethod == null) {
            return;
        }
        if (targetMethod.getParameterTypes().length == 0) {
            return;
        }

        Class<?> requestType = targetMethod.getParameterTypes()[0];
        Message protoMessage = parseProtoMessage(decodedData, requestType);
        if (protoMessage != null) {
            protoBidiRequestObserver.onNext(protoMessage);
        }
    }

    /**
     * Process POJO bidirectional streaming message.
     */
    private void processPojoBidiMessage(byte[] decodedData) {
        if (bidiRequestObserver == null) {
            return;
        }

        Request tripleRequest = parseRequest(decodedData);
        if (tripleRequest == null || tripleRequest.getArgsCount() == 0) {
            return;
        }

        String typeName = tripleRequest.getArgTypesList().get(0);
        Class<?> argType = ClassTypeUtils.getClass(typeName);
        byte[] argData = tripleRequest.getArgs(0).toByteArray();
        Object arg = serializer.decode(new ByteArrayWrapperByteBuf(argData), argType, null);
        bidiRequestObserver.onNext(arg);
    }

    /**
     * Notify bidirectional streaming error.
     */
    private void notifyBidiError(Exception e) {
        if (isProtoService && protoBidiRequestObserver != null) {
            protoBidiRequestObserver.onError(e);
        } else if (bidiRequestObserver != null) {
            bidiRequestObserver.onError(e);
        }
    }

    // ==================== Response Handling ====================

    /**
     * Handle unary response.
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

        Response tripleResponse = buildResponse(ret, declaredMethod.getReturnType(), tripleRequest.getSerializeType());
        writeResponse(tripleResponse, true);
    }

    /**
     * Handle bidirectional streaming.
     */
    @SuppressWarnings("unchecked")
    protected void handleBidiStreaming(SofaRequest request, SofaResponse response, Request tripleRequest) {
        if (response == null || response.getAppResponse() == null) {
            writeError(new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, "Bidi streaming response is null"));
            return;
        }

        bidiRequestObserver = (SofaStreamObserver<Object>) response.getAppResponse();
        processPendingBidiData();

        if (streamComplete) {
            bidiRequestObserver.onCompleted();
        }
    }

    /**
     * Build Response protobuf from object.
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

    // ==================== Stream Observers ====================

    /**
     * Create a StreamObserver for protobuf service responses.
     */
    protected StreamObserver<Object> createProtoStreamObserver() {
        return new StreamObserver<Object>() {
            @Override
            public void onNext(Object message) {
                if (message instanceof Message) {
                    byte[] data = ((Message) message).toByteArray();
                    byte[] grpcFrame = encodeGrpcFrame(data);
                    writeGrpcDataFrame(grpcFrame);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                writeError(throwable);
            }

            @Override
            public void onCompleted() {
                writeGrpcTrailers(0, null);
            }
        };
    }

    /**
     * Create server stream observer for server streaming calls.
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

    // ==================== HTTP Response Writing ====================

    /**
     * Write response to client.
     */
    protected void writeResponse(Response response) {
        writeResponse(response, false);
    }

    /**
     * Write response to client.
     */
    protected void writeResponse(Response response, boolean endOfStream) {
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            byte[] data = response.toByteArray();
            byte[] grpcFrame = encodeGrpcFrame(data);

            writeResponseHeadersIfNeeded();
            writeGrpcDataFrame(grpcFrame);

            if (endOfStream) {
                writeGrpcTrailers(0, null);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write response", e);
            onError(e);
        }
    }

    /**
     * Write response headers if not already sent.
     */
    private void writeResponseHeadersIfNeeded() {
        if (headersSent) {
            return;
        }

        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set(":status", HTTP_STATUS_OK);
        headers.set(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);
        httpChannel.writeHeader(new SimpleHttpMetadata(headers));
        headersSent = true;
    }

    /**
     * Write gRPC data frame.
     */
    private void writeGrpcDataFrame(byte[] grpcFrame) {
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        writeResponseHeadersIfNeeded();

        HttpOutputMessage outputMessage = httpChannel.newOutputMessage();
        outputMessage.setBody(grpcFrame);
        httpChannel.writeMessage(outputMessage);
    }

    /**
     * Write gRPC trailers.
     */
    protected void writeGrpcTrailers(int status, String message) {
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            DefaultHttpHeaders trailers = new DefaultHttpHeaders();
            trailers.set(GRPC_STATUS_HEADER, String.valueOf(status));
            if (message != null) {
                trailers.set(GRPC_MESSAGE_HEADER, message);
            }
            httpChannel.writeHeader(new SimpleHttpMetadata(trailers));
        } catch (Exception e) {
            LOGGER.error("Failed to write trailers", e);
        }
    }

    /**
     * Write error response.
     */
    protected void writeError(Throwable throwable) {
        LOGGER.error("Writing error response", throwable);
        if (httpChannel == null || !httpChannel.isActive()) {
            return;
        }

        try {
            io.grpc.Status status = io.grpc.Status.fromThrowable(throwable);
            writeGrpcTrailers(status.getCode().value(), status.getDescription());
        } catch (Exception e) {
            LOGGER.error("Failed to write error response", e);
            httpChannel.close();
        }
    }

    // ==================== gRPC Frame Encoding/Decoding ====================

    /**
     * Encode data in gRPC frame format.
     * Format: 1 byte compression flag + 4 bytes length + data
     */
    protected byte[] encodeGrpcFrame(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        byte[] frame = new byte[GRPC_FRAME_HEADER_SIZE + data.length];
        frame[0] = 0; // No compression

        // Write length as 4 bytes big-endian
        frame[1] = (byte) ((data.length >> 24) & 0xFF);
        frame[2] = (byte) ((data.length >> 16) & 0xFF);
        frame[3] = (byte) ((data.length >> 8) & 0xFF);
        frame[4] = (byte) (data.length & 0xFF);

        System.arraycopy(data, 0, frame, GRPC_FRAME_HEADER_SIZE, data.length);
        return frame;
    }

    /**
     * Decode gRPC framed data.
     */
    protected byte[] decodeGrpcFrame(byte[] data) {
        if (data == null || data.length < GRPC_FRAME_HEADER_SIZE) {
            return data;
        }

        int length = readGrpcFrameLength(data, 0);
        if (data.length >= GRPC_FRAME_HEADER_SIZE + length) {
            byte[] payload = new byte[length];
            System.arraycopy(data, GRPC_FRAME_HEADER_SIZE, payload, 0, length);
            return payload;
        }

        return data;
    }

    /**
     * Decode all gRPC frames from data.
     */
    protected List<byte[]> decodeAllGrpcFrames(byte[] data) {
        List<byte[]> payloads = new ArrayList<>();
        if (data == null || data.length < GRPC_FRAME_HEADER_SIZE) {
            return payloads;
        }

        int offset = 0;
        while (offset + GRPC_FRAME_HEADER_SIZE <= data.length) {
            int length = readGrpcFrameLength(data, offset);

            if (offset + GRPC_FRAME_HEADER_SIZE + length > data.length) {
                break;
            }

            byte[] payload = new byte[length];
            System.arraycopy(data, offset + GRPC_FRAME_HEADER_SIZE, payload, 0, length);
            payloads.add(payload);

            offset += GRPC_FRAME_HEADER_SIZE + length;
        }

        return payloads;
    }

    /**
     * Read gRPC frame length from data at offset.
     */
    private int readGrpcFrameLength(byte[] data, int offset) {
        return ((data[offset + 1] & 0xFF) << 24) |
            ((data[offset + 2] & 0xFF) << 16) |
            ((data[offset + 3] & 0xFF) << 8) |
            (data[offset + 4] & 0xFF);
    }

    // ==================== Utility Methods ====================

    /**
     * Build SofaRequest from HTTP metadata.
     */
    protected SofaRequest buildSofaRequest(Http3Metadata metadata) {
        SofaRequest request = new SofaRequest();
        String path = metadata.path();

        request.setInterfaceName(extractServiceName(path));
        request.setMethodName(extractMethodName(path));
        parseHeaders(request, metadata.headers());

        return request;
    }

    /**
     * Parse Request protobuf from bytes.
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
     * Parse protobuf message from bytes.
     */
    protected Message parseProtoMessage(byte[] data, Class<?> messageType) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            Method parseFrom = messageType.getMethod("parseFrom", byte[].class);
            return (Message) parseFrom.invoke(null, data);
        } catch (Exception e) {
            LOGGER.error("Failed to parse protobuf message of type: " + messageType.getName(), e);
            return null;
        }
    }

    /**
     * Find method on the service implementation class.
     */
    protected Method findProtoMethod(Class<?> serviceClass, String methodName) {
        // First try exact match
        for (Method method : serviceClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        // Try case-insensitive match
        String lowerMethodName = methodName.toLowerCase();
        for (Method method : serviceClass.getMethods()) {
            if (method.getName().toLowerCase().equals(lowerMethodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Determine call type from method signature.
     */
    protected String determineCallType(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            Class<?> lastParamType = paramTypes[paramTypes.length - 1];
            if (SofaStreamObserver.class.isAssignableFrom(lastParamType) ||
                StreamObserver.class.isAssignableFrom(lastParamType)) {
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
     * Set request parameters from triple request.
     */
    protected void setRequestParams(SofaRequest request, Request tripleRequest, Method declaredMethod) {
        Class<?>[] argTypes = getArgTypes(tripleRequest, callType);
        Object[] invokeArgs = buildInvokeArgs(tripleRequest, argTypes);

        request.setMethod(declaredMethod);
        request.setMethodArgs(invokeArgs);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
    }

    /**
     * Get argument types from request.
     */
    protected Class<?>[] getArgTypes(Request request, String callType) {
        ProtocolStringList argTypesList = request.getArgTypesList();
        boolean isStreaming = RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType) ||
            RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType);
        int size = isStreaming ? argTypesList.size() + 1 : argTypesList.size();
        Class<?>[] argTypes = new Class[size];

        for (int i = 0; i < argTypesList.size(); i++) {
            argTypes[i] = ClassTypeUtils.getClass(argTypesList.get(i));
        }

        if (isStreaming) {
            argTypes[size - 1] = SofaStreamObserver.class;
        }
        return argTypes;
    }

    /**
     * Build invoke arguments from request.
     */
    private Object[] buildInvokeArgs(Request tripleRequest, Class<?>[] argTypes) {
        if (RpcConstants.INVOKER_TYPE_BI_STREAMING.equals(callType)) {
            return new Object[1];
        }

        List<ByteString> argsList = tripleRequest.getArgsList();
        Object[] args = new Object[argsList.size()];

        for (int i = 0; i < argsList.size(); i++) {
            byte[] data = argsList.get(i).toByteArray();
            args[i] = serializer.decode(new ByteArrayWrapperByteBuf(data), argTypes[i], null);
        }

        if (RpcConstants.INVOKER_TYPE_SERVER_STREAMING.equals(callType)) {
            Object[] newArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            return newArgs;
        }

        return args;
    }

    /**
     * Extract service name from path.
     */
    protected String extractServiceName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        return slashIndex > 0 ? path.substring(0, slashIndex) : path;
    }

    /**
     * Extract method name from path.
     */
    protected String extractMethodName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        return slashIndex > 0 && slashIndex < path.length() - 1 ? path.substring(slashIndex + 1) : "";
    }

    /**
     * Parse headers into SofaRequest.
     */
    protected void parseHeaders(SofaRequest request, HttpHeaders headers) {
        if (headers == null) {
            return;
        }

        String timeoutStr = headers.get("grpc-timeout");
        if (timeoutStr != null) {
            request.setTimeout(parseGrpcTimeout(timeoutStr));
        }

        String serialization = headers.get("tri-serialize-type");
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
     */
    protected int parseGrpcTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.isEmpty()) {
            return 0;
        }

        try {
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
     * Safe close message.
     */
    private void safeClose(Http3InputMessage message) {
        try {
            message.close();
        } catch (Exception e) {
            onError(e);
        }
    }

    /**
     * Check if this listener supports the given content type.
     */
    public boolean supportsContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(GRPC_CONTENT_TYPE) || contentType.startsWith(JSON_CONTENT_TYPE);
    }

    // ==================== Inner Classes ====================

    /**
     * HTTP message listener interface.
     */
    @FunctionalInterface
    protected interface HttpMessageListener {
        void onMessage(byte[] data);
    }

    /**
     * Simple HttpMetadata implementation for writing headers.
     */
    private static class SimpleHttpMetadata implements HttpMetadata {
        private final HttpHeaders headers;

        SimpleHttpMetadata(HttpHeaders headers) {
            this.headers = headers;
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public String method() {
            return "POST";
        }

        @Override
        public String path() {
            return "";
        }

        @Override
        public HttpVersion httpVersion() {
            return HttpVersion.HTTP_3;
        }
    }
}