package triple;

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
    value = "by gRPC proto compiler (version 1.27.2)",
    comments = "Source: transformer.proto")
public final class GenericServiceGrpc {

  private GenericServiceGrpc() {}

  public static final String SERVICE_NAME = "GenericService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<triple.Request,
      triple.Response> getGenericMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "generic",
      requestType = triple.Request.class,
      responseType = triple.Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<triple.Request,
      triple.Response> getGenericMethod() {
    io.grpc.MethodDescriptor<triple.Request, triple.Response> getGenericMethod;
    if ((getGenericMethod = GenericServiceGrpc.getGenericMethod) == null) {
      synchronized (GenericServiceGrpc.class) {
        if ((getGenericMethod = GenericServiceGrpc.getGenericMethod) == null) {
          GenericServiceGrpc.getGenericMethod = getGenericMethod =
              io.grpc.MethodDescriptor.<triple.Request, triple.Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "generic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  triple.Request.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  triple.Response.getDefaultInstance()))
              .setSchemaDescriptor(new GenericServiceMethodDescriptorSupplier("generic"))
              .build();
        }
      }
    }
    return getGenericMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GenericServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericServiceStub>() {
        @java.lang.Override
        public GenericServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericServiceStub(channel, callOptions);
        }
      };
    return GenericServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GenericServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericServiceBlockingStub>() {
        @java.lang.Override
        public GenericServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericServiceBlockingStub(channel, callOptions);
        }
      };
    return GenericServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GenericServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericServiceFutureStub>() {
        @java.lang.Override
        public GenericServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericServiceFutureStub(channel, callOptions);
        }
      };
    return GenericServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class GenericServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void generic(triple.Request request,
        io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
      asyncUnimplementedUnaryCall(getGenericMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGenericMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                triple.Request,
                triple.Response>(
                  this, METHODID_GENERIC)))
          .build();
    }
  }

  /**
   */
  public static final class GenericServiceStub extends io.grpc.stub.AbstractAsyncStub<GenericServiceStub> {
    private GenericServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericServiceStub(channel, callOptions);
    }

    /**
     */
    public void generic(triple.Request request,
        io.grpc.stub.StreamObserver<triple.Response> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenericMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class GenericServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<GenericServiceBlockingStub> {
    private GenericServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public triple.Response generic(triple.Request request) {
      return blockingUnaryCall(
          getChannel(), getGenericMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class GenericServiceFutureStub extends io.grpc.stub.AbstractFutureStub<GenericServiceFutureStub> {
    private GenericServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<triple.Response> generic(
        triple.Request request) {
      return futureUnaryCall(
          getChannel().newCall(getGenericMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GENERIC = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GenericServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GenericServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GENERIC:
          serviceImpl.generic((triple.Request) request,
              (io.grpc.stub.StreamObserver<triple.Response>) responseObserver);
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
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class GenericServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GenericServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return triple.GenericProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GenericService");
    }
  }

  private static final class GenericServiceFileDescriptorSupplier
      extends GenericServiceBaseDescriptorSupplier {
    GenericServiceFileDescriptorSupplier() {}
  }

  private static final class GenericServiceMethodDescriptorSupplier
      extends GenericServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GenericServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (GenericServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GenericServiceFileDescriptorSupplier())
              .addMethod(getGenericMethod())
              .build();
        }
      }
    }
    return result;
  }
}
