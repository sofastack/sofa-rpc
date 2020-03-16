package com.alipay.sofa.rpc.grpc;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterGrpc;

public class SingleGrpcDemo {
    static final Logger LOGGER = LoggerFactory.getLogger(SingleGrpcDemo.class);

    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("grpc-server");

        int port = 50052;
        if (args.length != 0) {
            LOGGER.debug("first arg is {}", args[0]);
            port = Integer.valueOf(args[0]);
        }

        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
                .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
                .setPort(port);

        ProviderConfig<SofaGreeterGrpc.IGreeter> providerConfig = new ProviderConfig<SofaGreeterGrpc.IGreeter>()
                .setApplication(applicationConfig)
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_GRPC)
                .setInterfaceId(SofaGreeterGrpc.IGreeter.class.getName())
                .setRef(new GrpcGreeterImpl())
                .setServer(serverConfig)
                .setRegistry(registryConfig);

        providerConfig.export();


        ConsumerConfig<SofaGreeterGrpc.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterGrpc.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterGrpc.IGreeter.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
                .setRegistry(registryConfig);

        SofaGreeterGrpc.IGreeter greeterBlockingStub = consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", greeterBlockingStub.getClass().getName());

        LOGGER.info("Will try to greet " + "world" + " ...");
        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
                .build();
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        HelloReply reply = null;
        try {
            try {
                HelloRequest.DateTime reqDateTime = HelloRequest.DateTime.newBuilder(dateTime).setTime("")
                        .build();
                request = HelloRequest.newBuilder(request).setName("world").setDateTime(reqDateTime).build();
                reply = greeterBlockingStub.sayHello(request);
                LOGGER.info("Invoke Success,Greeting: {}, {}", reply.getMessage(), reply.getDateTime().getDate());
            } catch (StatusRuntimeException e) {
                LOGGER.error("RPC failed: {}", e.getStatus());
            } catch (Throwable e) {
                LOGGER.error("Unexpected RPC call breaks", e);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected RPC call breaks", e);
        }

    }
}
