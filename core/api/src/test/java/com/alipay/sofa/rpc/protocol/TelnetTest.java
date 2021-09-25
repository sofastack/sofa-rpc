package com.alipay.sofa.rpc.protocol;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.protocol.event.ApplicationEvent;
import com.alipay.sofa.rpc.protocol.listener.ConsumerSubEventListener;
import com.alipay.sofa.rpc.protocol.listener.ProviderPubEventListener;
import com.alipay.sofa.rpc.protocol.service.EchoService;
import com.alipay.sofa.rpc.protocol.service.EchoServiceImpl;
import com.alipay.sofa.rpc.protocol.service.HelloService;
import com.alipay.sofa.rpc.protocol.service.HelloServiceImpl;
import org.junit.Test;

public class TelnetTest {

    @Test
    public void telnet() {
        ProviderPubEventListener providerPubEventListener = new ProviderPubEventListener();
        ConsumerSubEventListener consumerSubEventListener = new ConsumerSubEventListener();
        ApplicationEvent applicationEvent = new ApplicationEvent();
        applicationEvent.addProviderPubEventListener(providerPubEventListener);
        applicationEvent.addConsumerSubEventListener(consumerSubEventListener);

        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
                .setPort(22000)
                .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setApplication(application)
                .setRef(new HelloServiceImpl())
                .setServer(serverConfig)
                .setRegister(false);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
                .setInterfaceId(EchoService.class.getName())
                .setApplication(application)
                .setRef(new EchoServiceImpl())
                .setServer(serverConfig)
                .setRegister(false);

        providerConfig.export();
        providerConfig2.export();

        //引用服务
        ApplicationConfig application2 = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
                .setApplication(application2)
                .setInterfaceId(HelloService.class.getName())
                .setDirectUrl("bolt://127.0.0.1:22000")
                .setRegister(false)
                .setTimeout(3000);
        HelloService helloService = consumerConfig.refer();

        applicationEvent.notifyProviderPubEvent(new ProviderPubEvent(providerConfig));
        applicationEvent.notifyProviderPubEvent(new ProviderPubEvent(providerConfig2));
        applicationEvent.notifyConsumerSubEvent(new ConsumerSubEvent(consumerConfig));

        NettyTelnetServer nettyTelnetServer = new NettyTelnetServer(1234);
        try {
            nettyTelnetServer.open();

        } catch (InterruptedException e) {
            nettyTelnetServer.close();
        }

    }

}