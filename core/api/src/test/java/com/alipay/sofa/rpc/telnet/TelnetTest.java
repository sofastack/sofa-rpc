package com.alipay.sofa.rpc.telnet;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.module.Module;
import com.alipay.sofa.rpc.telnet.module.TelnetModule;
import com.alipay.sofa.rpc.telnet.service.EchoService;
import com.alipay.sofa.rpc.telnet.service.EchoServiceImpl;
import com.alipay.sofa.rpc.telnet.service.HelloService;
import com.alipay.sofa.rpc.telnet.service.HelloServiceImpl;
import org.junit.Before;
import org.junit.Test;

public class statusTelnetTest {
    @Before
    public void init(){
        //发布服务，publishHelloService，发布Hello服务的方法
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

        TelnetModule module = (TelnetModule) ExtensionLoaderFactory.getExtensionLoader(Module.class)
                .getExtension("telnet");

        ProviderPubEvent providerPubEvent = new ProviderPubEvent(providerConfig);
        ProviderPubEvent providerPubEvent2 = new ProviderPubEvent(providerConfig2);
        ConsumerSubEvent consumerSubEvent = new ConsumerSubEvent(consumerConfig);
        EventBus.post(providerPubEvent);
        EventBus.post(providerPubEvent2);
        EventBus.post(consumerSubEvent);
    }

    @Test
    public void telnet() {
        //启动telnet服务端
        NettyTelnetServer nettyTelnetServer = new NettyTelnetServer(1234);
        try {
            nettyTelnetServer.open();
        } catch (InterruptedException interruptedException) {
            nettyTelnetServer.close();
        }
    }

}