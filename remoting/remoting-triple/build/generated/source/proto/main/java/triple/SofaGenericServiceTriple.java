    package triple;

import java.util.concurrent.TimeUnit;

import static triple.GenericServiceGrpc.getServiceDescriptor;
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

protected int timeout;

protected GenericServiceGrpc.GenericServiceBlockingStub blockingStub;
protected GenericServiceGrpc.GenericServiceFutureStub futureStub;
protected GenericServiceGrpc.GenericServiceStub stub;

public SofaGenericServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions, int timeout) {
this.timeout = timeout;

blockingStub = GenericServiceGrpc.newBlockingStub(channel).build(channel, callOptions);
futureStub = GenericServiceGrpc.newFutureStub(channel).build(channel, callOptions);
stub = GenericServiceGrpc.newStub(channel).build(channel, callOptions);
}

    public triple.Response generic(triple.Request request) {
    return blockingStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request);
    }

    public com.google.common.util.concurrent.ListenableFuture<triple.Response> genericAsync(triple.Request request) {
    return futureStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request);
    }

    public void generic(triple.Request request, io.grpc.stub.StreamObserver<triple.Response> responseObserver){
    stub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .generic(request, responseObserver);
    }

    public java.util.Iterator<triple.Response> genericServerStream(triple.Request request) {
    return blockingStub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .genericServerStream(request);
    }

    public void genericServerStream(triple.Request request, io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
    stub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .genericServerStream(request, responseObserver);
    }

    public io.grpc.stub.StreamObserver<triple.Request> genericBiStream(io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
    return stub
    .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    .genericBiStream(responseObserver);
    }
}

public static SofaGenericServiceStub getSofaStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions,int timeout) {
return new SofaGenericServiceStub(channel, callOptions, timeout);
}

public static String getServiceName() {
  return triple.GenericServiceGrpc.SERVICE_NAME;
}

public interface IGenericService {
    default public triple.Response generic(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    default public com.google.common.util.concurrent.ListenableFuture<triple.Response> genericAsync(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    public void generic(triple.Request request, io.grpc.stub.StreamObserver<triple.Response> responseObserver);

    default public java.util.Iterator<triple.Response> genericServerStream(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    public void genericServerStream(triple.Request request, io.grpc.stub.StreamObserver<triple.Response> responseObserver);

    public io.grpc.stub.StreamObserver<triple.Request> genericBiStream(io.grpc.stub.StreamObserver<triple.Response> responseObserver);

}

public static abstract class GenericServiceImplBase implements io.grpc.BindableService, IGenericService {

private IGenericService proxiedImpl;

public final void setProxiedImpl(IGenericService proxiedImpl) {
this.proxiedImpl = proxiedImpl;
}

    @java.lang.Override
    public final triple.Response generic(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    @java.lang.Override
    public final com.google.common.util.concurrent.ListenableFuture<triple.Response> genericAsync(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

    @java.lang.Override
    public final java.util.Iterator<triple.Response> genericServerStream(triple.Request request) {
    throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }

        public void generic(triple.Request request,
        io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
        asyncUnimplementedUnaryCall(triple.GenericServiceGrpc.getGenericMethod(), responseObserver);
        }
        public io.grpc.stub.StreamObserver<triple.Request> genericBiStream(
        io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
        return asyncUnimplementedStreamingCall(triple.GenericServiceGrpc.getGenericBiStreamMethod(), responseObserver);
        }
        public void genericServerStream(triple.Request request,
        io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
        asyncUnimplementedUnaryCall(triple.GenericServiceGrpc.getGenericServerStreamMethod(), responseObserver);
        }

@java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
    .addMethod(
    triple.GenericServiceGrpc.getGenericMethod(),
    asyncUnaryCall(
    new MethodHandlers<
    triple.Request,
    triple.Response>(
    proxiedImpl, METHODID_GENERIC)))
    .addMethod(
    triple.GenericServiceGrpc.getGenericBiStreamMethod(),
    asyncBidiStreamingCall(
    new MethodHandlers<
    triple.Request,
    triple.Response>(
    proxiedImpl, METHODID_GENERIC_BI_STREAM)))
    .addMethod(
    triple.GenericServiceGrpc.getGenericServerStreamMethod(),
    asyncServerStreamingCall(
    new MethodHandlers<
    triple.Request,
    triple.Response>(
    proxiedImpl, METHODID_GENERIC_SERVER_STREAM)))
.build();
}
}
    private static final int METHODID_GENERIC = 0;
    private static final int METHODID_GENERIC_BI_STREAM = 1;
    private static final int METHODID_GENERIC_SERVER_STREAM = 2;

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
            serviceImpl.generic((triple.Request) request,
            (io.grpc.stub.StreamObserver<triple.Response>) responseObserver);
            break;
            case METHODID_GENERIC_SERVER_STREAM:
            serviceImpl.genericServerStream((triple.Request) request,
            (io.grpc.stub.StreamObserver<triple.Response>) responseObserver);
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
                    case METHODID_GENERIC_BI_STREAM:
                    return (io.grpc.stub.StreamObserver
                <Req>) serviceImpl.genericBiStream(
                    (io.grpc.stub.StreamObserver<triple.Response>) responseObserver);
            default:
            throw new java.lang.AssertionError();
           }
      }
  }

}
