    package io.grpc.examples.helloworld;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.concurrent.TimeUnit;

import static io.grpc.examples.helloworld.GreeterGrpc.getServiceDescriptor;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

@javax.annotation.Generated(
value = "by SofaGrpc generator",
comments = "Source: helloworld.proto")
public final class SofaGreeterGrpc {
private SofaGreeterGrpc() {}

public static class SofaGreeterStub implements IGreeter {

protected ProviderInfo providerInfo;
protected ConsumerConfig consumerConfig;
protected int timeout;

protected GreeterGrpc.GreeterBlockingStub blockingStub;
protected GreeterGrpc.GreeterFutureStub futureStub;
protected GreeterGrpc.GreeterStub stub;

public SofaGreeterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions,ProviderInfo providerInfo, ConsumerConfig consumerConfig, int timeout) {
this.providerInfo = providerInfo;
this.consumerConfig = consumerConfig;
this.timeout = timeout;

blockingStub = GreeterGrpc.newBlockingStub(channel).build(channel, callOptions);
futureStub = GreeterGrpc.newFutureStub(channel).build(channel, callOptions);
stub = GreeterGrpc.newStub(channel).build(channel, callOptions);
}

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    public io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
    return blockingStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .sayHello(request);
    }

    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(io.grpc.examples.helloworld.HelloRequest request) {
    return futureStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .sayHello(request);
    }

    public void sayHello(io.grpc.examples.helloworld.HelloRequest request, io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver){
    stub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .sayHello(request, responseObserver);
    }

}

public static SofaGreeterStub getSofaStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions, ProviderInfo providerInfo, ConsumerConfig consumerConfig, int timeout) {
return new SofaGreeterStub(channel, callOptions, providerInfo, consumerConfig, timeout);
}

public interface IGreeter {
        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    default public io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    default public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(io.grpc.examples.helloworld.HelloRequest request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    public void sayHello(io.grpc.examples.helloworld.HelloRequest request, io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver);

}

    /**
     * <pre>
     *  The greeting service definition.
     * </pre>
     */
public static abstract class GreeterImplBase implements io.grpc.BindableService, IGreeter {

private IGreeter proxiedImpl;

public final void setProxiedImpl(IGreeter proxiedImpl) {
this.proxiedImpl = proxiedImpl;
}

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    @java.lang.Override
    public final io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
    @java.lang.Override
    public final com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(io.grpc.examples.helloworld.HelloRequest request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        public void sayHello(io.grpc.examples.helloworld.HelloRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver) {
        asyncUnimplementedUnaryCall(io.grpc.examples.helloworld.GreeterGrpc.getSayHelloMethod(), responseObserver);
        }

@java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
    .addMethod(
    io.grpc.examples.helloworld.GreeterGrpc.getSayHelloMethod(),
    asyncUnaryCall(
    new MethodHandlers<
    io.grpc.examples.helloworld.HelloRequest,
    io.grpc.examples.helloworld.HelloReply>(
    proxiedImpl, METHODID_SAY_HELLO)))
.build();
}
}
    private static final int METHODID_SAY_HELLO = 0;

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
private final IGreeter serviceImpl;
private final int methodId;

MethodHandlers(IGreeter serviceImpl, int methodId) {
this.serviceImpl = serviceImpl;
this.methodId = methodId;
}

@java.lang.Override
@java.lang.SuppressWarnings("unchecked")
public void invoke(Req request, io.grpc.stub.StreamObserver
<Resp> responseObserver) {
    switch (methodId) {
            case METHODID_SAY_HELLO:
            serviceImpl.sayHello((io.grpc.examples.helloworld.HelloRequest) request,
            (io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply>) responseObserver);
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
