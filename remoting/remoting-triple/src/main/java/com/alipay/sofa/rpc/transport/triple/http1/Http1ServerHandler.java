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
package com.alipay.sofa.rpc.transport.triple.http1;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.TripleContants;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;
import io.grpc.netty.shaded.io.netty.util.CharsetUtil;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Netty handler for HTTP/1.1 requests.
 * Simplified implementation that directly handles request/response.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http1ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger                          LOGGER             = LoggerFactory.getLogger(Http1ServerHandler.class);

    /** Default request timeout in milliseconds */
    private static final int                             DEFAULT_TIMEOUT_MS = 3000;

    /**
     * Server configuration
     */
    private final ServerConfig                           serverConfig;

    /**
     * Supplier for invoker map (to support dynamic service registration)
     */
    private final Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier;

    /**
     * Business thread pool executor
     */
    private final Executor                               bizExecutor;

    /**
     * Create a new HTTP/1.1 server handler.
     *
     * @param serverConfig server configuration
     * @param invokerMapSupplier supplier for invoker map
     * @param bizExecutor business thread pool executor
     */
    public Http1ServerHandler(ServerConfig serverConfig,
                              Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier,
                              Executor bizExecutor) {
        this.serverConfig = serverConfig;
        this.invokerMapSupplier = invokerMapSupplier;
        this.bizExecutor = bizExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // Validate request
        if (!request.decoderResult().isSuccess()) {
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Bad Request");
            return;
        }

        // Only allow POST method for RPC calls
        if (request.method() != HttpMethod.POST) {
            sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
            return;
        }

        String uri = request.uri();
        String path = uri;
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            path = uri.substring(0, queryIndex);
        }
        final String requestPath = path;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/1.1 request received: {} {} from {}", request.method(), requestPath, ctx.channel()
                .remoteAddress());
        }

        // Copy request body immediately before submitting to thread pool
        // (ByteBuf is released after channelRead0 returns)
        final byte[] requestBody = getRequestBody(request);
        final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        final String contentTypeFinal = contentType != null ? contentType : "application/json";

        // Extract uniqueId from HTTP header (for multi-service support)
        final String uniqueId = request.headers().get("tri-unique-id");

        // Submit to business thread pool for processing
        bizExecutor.execute(() -> {
            try {
                // Set uniqueId to RpcInvokeContext if present
                if (uniqueId != null && !uniqueId.isEmpty()) {
                    RpcInvokeContext.getContext().put(TripleContants.SOFA_UNIQUE_ID, uniqueId);
                } else {
                    // Clear any previous uniqueId
                    RpcInvokeContext.getContext().put(TripleContants.SOFA_UNIQUE_ID, "");
                }
                processRequest(ctx, requestPath, requestBody, contentTypeFinal);
            } catch (Exception e) {
                LOGGER.error("Error processing HTTP/1.1 request: {}", requestPath, e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage());
            } finally {
                // Clean up RpcInvokeContext
                RpcInvokeContext.removeContext();
            }
        });
    }

    /**
     * Process the HTTP request and send response.
     *
     * @param ctx channel handler context
     * @param path request path
     * @param body request body bytes
     * @param contentType content type
     */
    private void processRequest(ChannelHandlerContext ctx, String path, byte[] body, String contentType) {
        try {
            // Parse service and method from path
            String serviceName = extractServiceName(path);
            String methodName = extractMethodName(path);

            if (StringUtils.isBlank(serviceName) || StringUtils.isBlank(methodName)) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST,
                    "Invalid service path: " + path);
                return;
            }

            // Build SofaRequest
            SofaRequest sofaRequest = new SofaRequest();
            sofaRequest.setInterfaceName(serviceName);
            sofaRequest.setMethodName(methodName);
            sofaRequest.setTimeout(DEFAULT_TIMEOUT_MS);

            // Find invoker for this service
            Invoker targetInvoker = findInvoker(serviceName);
            if (targetInvoker == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND,
                    "Service not found: " + serviceName);
                return;
            }

            // Get the service class and method
            // The invoker is UniqueIdInvoker which wraps ProviderProxyInvoker
            Class<?> serviceClass = null;
            if (targetInvoker instanceof UniqueIdInvoker) {
                UniqueIdInvoker uniqueIdInvoker = (UniqueIdInvoker) targetInvoker;
                Invoker innerInvoker = uniqueIdInvoker.getInnerInvoker();
                if (innerInvoker instanceof com.alipay.sofa.rpc.server.ProviderProxyInvoker) {
                    com.alipay.sofa.rpc.server.ProviderProxyInvoker proxyInvoker =
                            (com.alipay.sofa.rpc.server.ProviderProxyInvoker) innerInvoker;
                    serviceClass = proxyInvoker.getProviderConfig().getProxyClass();
                }
            }

            if (serviceClass == null) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Cannot determine service class");
                return;
            }

            // Find the method on the service class
            // Note: gRPC method names in URL are typically PascalCase (e.g., SayHello)
            // but Java methods are camelCase (e.g., sayHello), so we try both
            Method targetMethod = null;
            String normalizedMethodName = normalizeMethodName(methodName);
            for (Method m : serviceClass.getMethods()) {
                if (m.getName().equals(normalizedMethodName) || m.getName().equals(methodName)) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND,
                    "Method not found: " + methodName);
                return;
            }

            // Check if method has StreamObserver parameter (gRPC style)
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            boolean hasStreamObserver = false;
            if (paramTypes.length > 0) {
                Class<?> lastParamType = paramTypes[paramTypes.length - 1];
                if (io.grpc.stub.StreamObserver.class.isAssignableFrom(lastParamType)) {
                    hasStreamObserver = true;
                } else if (SofaStreamObserver.class.isAssignableFrom(lastParamType)) {
                    hasStreamObserver = true;
                }
            }

            // Parse request body based on content type
            Object[] args = parseRequestBody(body, contentType, methodName, targetMethod);

            // If method has StreamObserver parameter, create an adapter to capture response
            Http1StreamObserverAdapter streamObserverAdapter = null;
            if (hasStreamObserver && args.length > 0 && args[args.length - 1] == null) {
                // Get the type parameter of StreamObserver (e.g., HelloReply)
                java.lang.reflect.Type genericReturnType = targetMethod.getGenericParameterTypes()[paramTypes.length - 1];
                Class<?> responseType = Object.class;
                if (genericReturnType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) genericReturnType).getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                        responseType = (Class<?>) typeArgs[0];
                    }
                }
                streamObserverAdapter = new Http1StreamObserverAdapter(responseType);
                args[args.length - 1] = streamObserverAdapter;
            }

            sofaRequest.setMethod(targetMethod);
            sofaRequest.setMethodArgs(args);
            sofaRequest.setMethodArgSigs(getClassNames(targetMethod.getParameterTypes()));

            // Invoke the service
            SofaResponse sofaResponse;
            try {
                Object result = targetInvoker.invoke(sofaRequest);
                if (result instanceof SofaResponse) {
                    sofaResponse = (SofaResponse) result;
                } else {
                    sofaResponse = new SofaResponse();
                    sofaResponse.setAppResponse(result);
                }
            } catch (Throwable t) {
                LOGGER.error("Service invocation failed: {}.{}", serviceName, methodName, t);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Service error: " + t.getMessage());
                return;
            }

            // Get the response
            Object appResponse;
            if (hasStreamObserver && streamObserverAdapter != null) {
                // Wait for the response from StreamObserver
                appResponse = streamObserverAdapter.waitForResponse();
            } else {
                appResponse = sofaResponse.getAppResponse();
            }

            // Send response
            byte[] responseBytes = serializeResponse(appResponse, contentType);

            sendSuccessResponse(ctx, responseBytes, contentType);

        } catch (Exception e) {
            LOGGER.error("Error processing request", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                "Error: " + e.getMessage());
        }
    }

    /**
     * Find invoker for the given service.
     * First tries exact match by interface name, then falls back to suffix match
     * to handle gRPC paths that use short service names.
     *
     * @param serviceName service interface name extracted from request path
     * @return invoker, or null if not found
     */
    private Invoker findInvoker(String serviceName) {
        Map<String, UniqueIdInvoker> invokerMap = invokerMapSupplier.get();
        if (invokerMap == null || invokerMap.isEmpty()) {
            return null;
        }

        // Try exact match first (e.g., "com.example.GreeterService")
        UniqueIdInvoker invoker = invokerMap.get(serviceName);
        if (invoker != null && invoker.hasInvoker()) {
            return invoker;
        }

        // Try suffix match to handle short names in gRPC paths (e.g., "helloworld.Greeter")
        for (Map.Entry<String, UniqueIdInvoker> entry : invokerMap.entrySet()) {
            String key = entry.getKey();
            if ((key.endsWith("." + serviceName) || key.endsWith("/" + serviceName))
                    && entry.getValue().hasInvoker()) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Parse request body based on content type.
     *
     * @param body request body bytes
     * @param contentType content type
     * @param methodName method name
     * @param method target method
     * @return method arguments
     */
    private Object[] parseRequestBody(byte[] body, String contentType, String methodName, Method method) {
        if (body == null || body.length == 0) {
            return new Object[0];
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return new Object[0];
        }

        // Determine the actual request parameter count
        // For gRPC-style methods, the last parameter is StreamObserver for response
        int requestParamCount = paramTypes.length;
        if (paramTypes.length > 0) {
            Class<?> lastParamType = paramTypes[paramTypes.length - 1];
            // Check if last parameter is StreamObserver (gRPC style)
            if (io.grpc.stub.StreamObserver.class.isAssignableFrom(lastParamType) ||
                SofaStreamObserver.class.isAssignableFrom(lastParamType)) {
                requestParamCount = paramTypes.length - 1;
            }
        }

        // If no request parameters (only StreamObserver), return empty args with null for StreamObserver
        if (requestParamCount == 0) {
            Object[] args = new Object[paramTypes.length];
            // StreamObserver will be set by the framework
            return args;
        }

        // For JSON content type
        if (contentType.contains("application/json")) {
            try {
                Object[] args = new Object[paramTypes.length];
                String jsonStr = new String(body, StandardCharsets.UTF_8);

                if (requestParamCount == 1) {
                    // Single request parameter - parse the JSON object directly
                    Class<?> requestType = paramTypes[0];
                    args[0] = parseJsonObject(jsonStr, requestType);
                } else {
                    // Multiple request parameters - try to parse as JSON array first
                    try {
                        com.alibaba.fastjson.JSONArray array = JSON.parseArray(jsonStr);
                        if (array != null && array.size() >= requestParamCount) {
                            for (int i = 0; i < requestParamCount; i++) {
                                Object item = array.get(i);
                                if (item instanceof com.alibaba.fastjson.JSONObject) {
                                    args[i] = parseJsonObject(((com.alibaba.fastjson.JSONObject) item).toJSONString(),
                                        paramTypes[i]);
                                } else {
                                    args[i] = JSON.parseObject(item.toString(), paramTypes[i]);
                                }
                            }
                        } else {
                            // Not a valid array, try parsing as single object for first parameter
                            args[0] = parseJsonObject(jsonStr, paramTypes[0]);
                        }
                    } catch (Exception e) {
                        // Not a JSON array, try parsing as single object for first parameter
                        args[0] = parseJsonObject(jsonStr, paramTypes[0]);
                    }
                }
                return args;
            } catch (Exception e) {
                LOGGER.error("Failed to parse JSON request body", e);
                // Fall through to default handling
            }
        }

        // Default: return as string for first parameter if it's String type
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < requestParamCount; i++) {
            if (paramTypes[i] == String.class) {
                args[i] = bodyStr;
            } else if (paramTypes[i] == byte[].class) {
                args[i] = body;
            }
        }
        return args;
    }

    /**
     * Parse JSON string to object, supporting both protobuf and regular POJO classes.
     *
     * @param jsonStr JSON string
     * @param targetType target class type
     * @return parsed object
     * @throws Exception if parsing fails
     */
    private Object parseJsonObject(String jsonStr, Class<?> targetType) throws Exception {
        // Check if it's a protobuf Message class
        if (Message.class.isAssignableFrom(targetType)) {
            return parseProtobufJson(jsonStr, targetType);
        }
        // Use fastjson for regular POJO classes
        return JSON.parseObject(jsonStr, targetType);
    }

    /**
     * Parse JSON string to protobuf Message object.
     *
     * @param jsonStr JSON string
     * @param messageType protobuf message class
     * @return parsed protobuf Message
     * @throws Exception if parsing fails
     */
    private Message parseProtobufJson(String jsonStr, Class<?> messageType) throws Exception {
        // Create a new builder instance using the default instance
        Message defaultInstance = (Message) messageType.getMethod("getDefaultInstance").invoke(null);
        Message.Builder builder = defaultInstance.newBuilderForType();

        // Parse JSON into the builder
        JsonFormat.parser().ignoringUnknownFields().merge(jsonStr, builder);

        return builder.build();
    }

    /**
     * Get class names from class array for method argument signatures.
     *
     * @param classes parameter types
     * @return class names
     */
    private String[] getClassNames(Class<?>[] classes) {
        if (classes == null || classes.length == 0) {
            return new String[0];
        }
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }

    /**
     * Serialize response based on content type.
     *
     * @param response response object
     * @param contentType content type
     * @return serialized bytes
     */
    private byte[] serializeResponse(Object response, String contentType) {
        if (response == null) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        }

        // For JSON content type
        if (contentType.contains("application/json")) {
            try {
                // Check if it's a protobuf Message
                if (response instanceof Message) {
                    return JsonFormat.printer().print((Message) response).getBytes(StandardCharsets.UTF_8);
                }
                // Use fastjson for regular POJO classes
                return JSON.toJSONBytes(response);
            } catch (Exception e) {
                LOGGER.error("Failed to serialize response to JSON", e);
                // Fall through to default handling
            }
        }

        // Default: return as string
        return response.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parse headers into SofaRequest.
     *
     * @param request SofaRequest
     * @param headers HTTP headers
     */
    private void parseHeaders(SofaRequest request, HttpHeaders headers) {
        // Parse timeout
        String timeoutStr = headers.get("tri-timeout");
        if (timeoutStr != null) {
            try {
                request.setTimeout(Integer.parseInt(timeoutStr));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Parse serialization type
        String serialization = headers.get("tri-serialize-type");
        if (serialization != null) {
            try {
                request.setSerializeType(Byte.parseByte(serialization));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    /**
     * Extract service name from path.
     *
     * @param path request path
     * @return service name
     */
    private String extractServiceName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Remove leading slash
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
    private String extractMethodName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Remove leading slash
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
     * Normalize method name from PascalCase to camelCase.
     * gRPC method names in URL are typically PascalCase (e.g., SayHello)
     * but Java methods are camelCase (e.g., sayHello).
     *
     * @param methodName method name from URL
     * @return normalized method name (camelCase)
     */
    private String normalizeMethodName(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return methodName;
        }
        // Convert first character to lowercase
        return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
    }

    /**
     * Get request body as byte array.
     *
     * @param request Netty full HTTP request
     * @return request body bytes
     */
    private byte[] getRequestBody(FullHttpRequest request) {
        ByteBuf content = request.content();
        if (content == null || !content.isReadable()) {
            return new byte[0];
        }

        byte[] body = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), body);
        return body;
    }

    /**
     * Send success response.
     *
     * @param ctx channel handler context
     * @param body response body
     * @param contentType content type
     */
    private void sendSuccessResponse(ChannelHandlerContext ctx, byte[] body, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
            Unpooled.wrappedBuffer(body));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, "close");

        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send error response.
     *
     * @param ctx channel handler context
     * @param status HTTP status
     * @param message error message
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status,
            Unpooled.copiedBuffer("{\"error\":\"" + escapeJson(message) + "\"}", CharsetUtil.UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, "close");

        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Escape special characters in JSON string.
     *
     * @param str input string
     * @return escaped string
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("HTTP/1.1 handler exception", cause);
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
        ctx.close();
    }

    /**
     * StreamObserver adapter for HTTP/1.1 calls.
     * Captures responses from gRPC-style methods and makes them available for HTTP response.
     */
    private class Http1StreamObserverAdapter implements io.grpc.stub.StreamObserver<Object>, SofaStreamObserver<Object> {

        private final List<Object>       responses      = new ArrayList<>();
        private final CountDownLatch     latch         = new CountDownLatch(1);
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final Class<?>           responseType;

        public Http1StreamObserverAdapter(Class<?> responseType) {
            this.responseType = responseType;
        }

        @Override
        public void onNext(Object value) {
            synchronized (responses) {
                responses.add(value);
            }
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }

        /**
         * Wait for the response and return it.
         * For unary calls, returns the single response.
         * For server streaming calls, returns the list of responses.
         *
         * @return the captured response(s)
         */
        public Object waitForResponse() {
            try {
                // Wait for completion with timeout
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    LOGGER.warn("StreamObserver timeout waiting for response");
                    return null;
                }

                // Check for error
                Throwable t = error.get();
                if (t != null) {
                    LOGGER.error("StreamObserver received error", t);
                    return t;
                }

                // Return the response(s)
                synchronized (responses) {
                    if (responses.isEmpty()) {
                        return null;
                    }
                    // For unary calls, return the single response
                    // For server streaming calls, return the first response (or could return list)
                    return responses.get(0);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while waiting for StreamObserver response", e);
                return null;
            }
        }
    }
}