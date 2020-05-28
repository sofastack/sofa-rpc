    package com.alibaba.triple.proto;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.concurrent.TimeUnit;

import static com.alibaba.triple.proto.GenericServiceGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

@javax.annotation.Generated(
value = "by SofaTriple generator",
comments = "Source: transformer.proto")
public final class SofaGenericServiceTriple {
private SofaGenericServiceTriple() {}

public static class SofaGenericServiceStub implements IGenericService {

protected ProviderInfo providerInfo;
protected ConsumerConfig consumerConfig;
protected int timeout;

protected GenericServiceGrpc.GenericServiceBlockingStub blockingStub;
protected GenericServiceGrpc.GenericServiceFutureStub futureStub;
protected GenericServiceGrpc.GenericServiceStub stub;

public SofaGenericServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions,ProviderInfo providerInfo, ConsumerConfig consumerConfig, int timeout) {
this.providerInfo = providerInfo;
this.consumerConfig = consumerConfig;
this.timeout = timeout;

blockingStub = GenericServiceGrpc.newBlockingStub(channel).build(channel, callOptions);
futureStub = GenericServiceGrpc.newFutureStub(channel).build(channel, callOptions);
stub = GenericServiceGrpc.newStub(channel).build(channel, callOptions);
}

    public com.alibaba.triple.proto.Response generic(com.alibaba.triple.proto.Request request) {
    return blockingStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request);
    }

    public com.google.common.util.concurrent.ListenableFuture<com.alibaba.triple.proto.Response> genericAsync(com.alibaba.triple.proto.Request request) {
    return futureStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request);
    }

    public void generic(com.alibaba.triple.proto.Request request, io.grpc.stub.StreamObserver<com.alibaba.triple.proto.Response> responseObserver){
    stub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request, responseObserver);
    }

}

public static SofaGenericServiceStub getSofaStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions, ProviderInfo providerInfo, ConsumerConfig consumerConfig, int timeout) {
return new SofaGenericServiceStub(channel, callOptions, providerInfo, consumerConfig, timeout);
}

public static String getServiceName() {
  return com.alibaba.triple.proto.GenericServiceGrpc.SERVICE_NAME;
}

public interface IGenericService {
    default public com.alibaba.triple.proto.Response generic(com.alibaba.triple.proto.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    default public com.google.common.util.concurrent.ListenableFuture<com.alibaba.triple.proto.Response> genericAsync(com.alibaba.triple.proto.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    public void generic(com.alibaba.triple.proto.Request request, io.grpc.stub.StreamObserver<com.alibaba.triple.proto.Response> responseObserver);

}

public static abstract class GenericServiceImplBase implements io.grpc.BindableService, IGenericService {

private IGenericService proxiedImpl;

public final void setProxiedImpl(IGenericService proxiedImpl) {
this.proxiedImpl = proxiedImpl;
}

    @java.lang.Override
    public final com.alibaba.triple.proto.Response generic(com.alibaba.triple.proto.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    @java.lang.Override
    public final com.google.common.util.concurrent.ListenableFuture<com.alibaba.triple.proto.Response> genericAsync(com.alibaba.triple.proto.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        public void generic(com.alibaba.triple.proto.Request request,
        io.grpc.stub.StreamObserver<com.alibaba.triple.proto.Response> responseObserver) {
        asyncUnimplementedUnaryCall(com.alibaba.triple.proto.GenericServiceGrpc.getGenericMethod(), responseObserver);
        }

@java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
    .addMethod(
    com.alibaba.triple.proto.GenericServiceGrpc.getGenericMethod(),
    asyncUnaryCall(
    new MethodHandlers<
    com.alibaba.triple.proto.Request,
    com.alibaba.triple.proto.Response>(
    proxiedImpl, METHODID_GENERIC)))
.build();
}
}
    private static final int METHODID_GENERIC = 0;

private static final class MethodHandlers
<Req, Resp> implements
io.grpc.stub.ServerCalls.UnaryMethod
<Req, Resp>,
io.grpc.stub.ServerCalls.ServerStreamingMethod
<Req, Resp>,
io.grpc.stub.ServerCalls.ClientStreamingMethod
<Req, Resp>,
io.grpc.stub.ServerCalls.BidiStreamingMethod
<Req, Resp> {
private final IGenericService serviceImpl;
private final int methodId;

MethodHandlers(IGenericService serviceImpl, int methodId) {
this.serviceImpl = serviceImpl;
this.methodId = methodId;
}

@java.lang.Override
@java.lang.SuppressWarnings("unchecked")
public void invoke(Req request, io.grpc.stub.StreamObserver
<Resp> responseObserver) {
    switch (methodId) {
            case METHODID_GENERIC:
            serviceImpl.generic((com.alibaba.triple.proto.Request) request,
            (io.grpc.stub.StreamObserver<com.alibaba.triple.proto.Response>) responseObserver);
            break;
    default:
    throw new java.lang.AssertionError();
    }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver
    <Req> invoke(io.grpc.stub.StreamObserver
        <Resp> responseObserver) {
            switch (methodId) {
            default:
            throw new java.lang.AssertionError();
           }
      }
  }

}
