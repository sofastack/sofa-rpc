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
package com.alipay.sofa.rpc.model.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.16.1)",
        comments = "Source: GrpcTestService.proto")
public final class GrpcTestServiceGrpc {

    private GrpcTestServiceGrpc() {
    }

    public static final String                                                                                                                                                      SERVICE_NAME = "GrpcTestService";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "reqString",
            requestType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.class,
            responseType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
            com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringMethod() {
        io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringMethod;
        if ((getReqStringMethod = GrpcTestServiceGrpc.getReqStringMethod) == null) {
            synchronized (GrpcTestServiceGrpc.class) {
                if ((getReqStringMethod = GrpcTestServiceGrpc.getReqStringMethod) == null) {
                    GrpcTestServiceGrpc.getReqStringMethod = getReqStringMethod =
                            io.grpc.MethodDescriptor
                                .<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> newBuilder()
                                .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                .setFullMethodName(generateFullMethodName(
                                    "GrpcTestService", "reqString"))
                                .setSampledToLocalTracing(true)
                                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.getDefaultInstance()))
                                .setResponseMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                        com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String
                                            .getDefaultInstance()))
                                .setSchemaDescriptor(new GrpcTestServiceMethodDescriptorSupplier("reqString"))
                                .build();
                }
            }
        }
        return getReqStringMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringClientStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "reqStringClientStream",
            requestType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.class,
            responseType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String.class,
            methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
    public static io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
            com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringClientStreamMethod() {
        io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringClientStreamMethod;
        if ((getReqStringClientStreamMethod = GrpcTestServiceGrpc.getReqStringClientStreamMethod) == null) {
            synchronized (GrpcTestServiceGrpc.class) {
                if ((getReqStringClientStreamMethod = GrpcTestServiceGrpc.getReqStringClientStreamMethod) == null) {
                    GrpcTestServiceGrpc.getReqStringClientStreamMethod = getReqStringClientStreamMethod =
                            io.grpc.MethodDescriptor
                                .<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> newBuilder()
                                .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
                                .setFullMethodName(generateFullMethodName(
                                    "GrpcTestService", "reqStringClientStream"))
                                .setSampledToLocalTracing(true)
                                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.getDefaultInstance()))
                                .setResponseMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                        com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String
                                            .getDefaultInstance()))
                                .setSchemaDescriptor(
                                    new GrpcTestServiceMethodDescriptorSupplier("reqStringClientStream"))
                                .build();
                }
            }
        }
        return getReqStringClientStreamMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringServerStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "reqStringServerStream",
            requestType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.class,
            responseType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String.class,
            methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
    public static io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
            com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringServerStreamMethod() {
        io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringServerStreamMethod;
        if ((getReqStringServerStreamMethod = GrpcTestServiceGrpc.getReqStringServerStreamMethod) == null) {
            synchronized (GrpcTestServiceGrpc.class) {
                if ((getReqStringServerStreamMethod = GrpcTestServiceGrpc.getReqStringServerStreamMethod) == null) {
                    GrpcTestServiceGrpc.getReqStringServerStreamMethod = getReqStringServerStreamMethod =
                            io.grpc.MethodDescriptor
                                .<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> newBuilder()
                                .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                                .setFullMethodName(generateFullMethodName(
                                    "GrpcTestService", "reqStringServerStream"))
                                .setSampledToLocalTracing(true)
                                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.getDefaultInstance()))
                                .setResponseMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                        com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String
                                            .getDefaultInstance()))
                                .setSchemaDescriptor(
                                    new GrpcTestServiceMethodDescriptorSupplier("reqStringServerStream"))
                                .build();
                }
            }
        }
        return getReqStringServerStreamMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringBothStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "reqStringBothStream",
            requestType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.class,
            responseType = com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String.class,
            methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
    public static io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
            com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringBothStreamMethod() {
        io.grpc.MethodDescriptor<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> getReqStringBothStreamMethod;
        if ((getReqStringBothStreamMethod = GrpcTestServiceGrpc.getReqStringBothStreamMethod) == null) {
            synchronized (GrpcTestServiceGrpc.class) {
                if ((getReqStringBothStreamMethod = GrpcTestServiceGrpc.getReqStringBothStreamMethod) == null) {
                    GrpcTestServiceGrpc.getReqStringBothStreamMethod = getReqStringBothStreamMethod =
                            io.grpc.MethodDescriptor
                                .<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String, com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> newBuilder()
                                .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
                                .setFullMethodName(generateFullMethodName(
                                    "GrpcTestService", "reqStringBothStream"))
                                .setSampledToLocalTracing(true)
                                .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String.getDefaultInstance()))
                                .setResponseMarshaller(
                                    io.grpc.protobuf.ProtoUtils.marshaller(
                                        com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String
                                            .getDefaultInstance()))
                                .setSchemaDescriptor(new GrpcTestServiceMethodDescriptorSupplier("reqStringBothStream"))
                                .build();
                }
            }
        }
        return getReqStringBothStreamMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static GrpcTestServiceStub newStub(io.grpc.Channel channel) {
        return new GrpcTestServiceStub(channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static GrpcTestServiceBlockingStub newBlockingStub(
                                                              io.grpc.Channel channel) {
        return new GrpcTestServiceBlockingStub(channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static GrpcTestServiceFutureStub newFutureStub(
                                                          io.grpc.Channel channel) {
        return new GrpcTestServiceFutureStub(channel);
    }

    /**
     */
    public static abstract class GrpcTestServiceImplBase implements io.grpc.BindableService {

        /**
         */
        public void reqString(com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request,
                              io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            asyncUnimplementedUnaryCall(getReqStringMethod(), responseObserver);
        }

        /**
         */
        public io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String> reqStringClientStream(
                                                                                                                                io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            return asyncUnimplementedStreamingCall(getReqStringClientStreamMethod(), responseObserver);
        }

        /**
         */
        public void reqStringServerStream(com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request,
                                          io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            asyncUnimplementedUnaryCall(getReqStringServerStreamMethod(), responseObserver);
        }

        /**
         */
        public io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String> reqStringBothStream(
                                                                                                                              io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            return asyncUnimplementedStreamingCall(getReqStringBothStreamMethod(), responseObserver);
        }

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                .addMethod(
                    getReqStringMethod(),
                    asyncUnaryCall(
                    new MethodHandlers<
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>(
                        this, METHODID_REQ_STRING)))
                .addMethod(
                    getReqStringClientStreamMethod(),
                    asyncClientStreamingCall(
                    new MethodHandlers<
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>(
                        this, METHODID_REQ_STRING_CLIENT_STREAM)))
                .addMethod(
                    getReqStringServerStreamMethod(),
                    asyncServerStreamingCall(
                    new MethodHandlers<
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>(
                        this, METHODID_REQ_STRING_SERVER_STREAM)))
                .addMethod(
                    getReqStringBothStreamMethod(),
                    asyncBidiStreamingCall(
                    new MethodHandlers<
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String,
                    com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>(
                        this, METHODID_REQ_STRING_BOTH_STREAM)))
                .build();
        }
    }

    /**
     */
    public static final class GrpcTestServiceStub extends io.grpc.stub.AbstractStub<GrpcTestServiceStub> {
        private GrpcTestServiceStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GrpcTestServiceStub(io.grpc.Channel channel,
                                    io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GrpcTestServiceStub build(io.grpc.Channel channel,
                                            io.grpc.CallOptions callOptions) {
            return new GrpcTestServiceStub(channel, callOptions);
        }

        /**
         */
        public void reqString(com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request,
                              io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            asyncUnaryCall(
                getChannel().newCall(getReqStringMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String> reqStringClientStream(
                                                                                                                                io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            return asyncClientStreamingCall(
                getChannel().newCall(getReqStringClientStreamMethod(), getCallOptions()), responseObserver);
        }

        /**
         */
        public void reqStringServerStream(com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request,
                                          io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            asyncServerStreamingCall(
                getChannel().newCall(getReqStringServerStreamMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String> reqStringBothStream(
                                                                                                                              io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver) {
            return asyncBidiStreamingCall(
                getChannel().newCall(getReqStringBothStreamMethod(), getCallOptions()), responseObserver);
        }
    }

    /**
     */
    public static final class GrpcTestServiceBlockingStub extends
                                                         io.grpc.stub.AbstractStub<GrpcTestServiceBlockingStub> {
        private GrpcTestServiceBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GrpcTestServiceBlockingStub(io.grpc.Channel channel,
                                            io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GrpcTestServiceBlockingStub build(io.grpc.Channel channel,
                                                    io.grpc.CallOptions callOptions) {
            return new GrpcTestServiceBlockingStub(channel, callOptions);
        }

        /**
         */
        public com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String reqString(com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request) {
            return blockingUnaryCall(
                getChannel(), getReqStringMethod(), getCallOptions(), request);
        }

        /**
         */
        public java.util.Iterator<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> reqStringServerStream(
                                                                                                                        com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request) {
            return blockingServerStreamingCall(
                getChannel(), getReqStringServerStreamMethod(), getCallOptions(), request);
        }
    }

    /**
     */
    public static final class GrpcTestServiceFutureStub extends io.grpc.stub.AbstractStub<GrpcTestServiceFutureStub> {
        private GrpcTestServiceFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GrpcTestServiceFutureStub(io.grpc.Channel channel,
                                          io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GrpcTestServiceFutureStub build(io.grpc.Channel channel,
                                                  io.grpc.CallOptions callOptions) {
            return new GrpcTestServiceFutureStub(channel, callOptions);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> reqString(
                                                                                                                                            com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String request) {
            return futureUnaryCall(
                getChannel().newCall(getReqStringMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_REQ_STRING               = 0;
    private static final int METHODID_REQ_STRING_SERVER_STREAM = 1;
    private static final int METHODID_REQ_STRING_CLIENT_STREAM = 2;
    private static final int METHODID_REQ_STRING_BOTH_STREAM   = 3;

    private static final class MethodHandlers<Req, Resp> implements
                                                         io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                                                         io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                                                         io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                                                         io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final GrpcTestServiceImplBase serviceImpl;
        private final int                     methodId;

        MethodHandlers(GrpcTestServiceImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_REQ_STRING:
                    serviceImpl
                        .reqString(
                            (com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String) request,
                            (io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>) responseObserver);
                    break;
                case METHODID_REQ_STRING_SERVER_STREAM:
                    serviceImpl
                        .reqStringServerStream(
                            (com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String) request,
                            (io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(
                                                       io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_REQ_STRING_CLIENT_STREAM:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl
                        .reqStringClientStream(
                        (io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>) responseObserver);
                case METHODID_REQ_STRING_BOTH_STREAM:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl
                        .reqStringBothStream(
                        (io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>) responseObserver);
                default:
                    throw new AssertionError();
            }
        }
    }

    private static abstract class GrpcTestServiceBaseDescriptorSupplier
                                                                       implements
                                                                       io.grpc.protobuf.ProtoFileDescriptorSupplier,
                                                                       io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        GrpcTestServiceBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return com.alipay.sofa.rpc.model.grpc.GrpcTestServiceProto.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("GrpcTestService");
        }
    }

    private static final class GrpcTestServiceFileDescriptorSupplier
                                                                    extends GrpcTestServiceBaseDescriptorSupplier {
        GrpcTestServiceFileDescriptorSupplier() {
        }
    }

    private static final class GrpcTestServiceMethodDescriptorSupplier
                                                                      extends GrpcTestServiceBaseDescriptorSupplier
                                                                                                                   implements
                                                                                                                   io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final String methodName;

        GrpcTestServiceMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (GrpcTestServiceGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                        .setSchemaDescriptor(new GrpcTestServiceFileDescriptorSupplier())
                        .addMethod(getReqStringMethod())
                        .addMethod(getReqStringClientStreamMethod())
                        .addMethod(getReqStringServerStreamMethod())
                        .addMethod(getReqStringBothStreamMethod())
                        .build();
                }
            }
        }
        return result;
    }
}
