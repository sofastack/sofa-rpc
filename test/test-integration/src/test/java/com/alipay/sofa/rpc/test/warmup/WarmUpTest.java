/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.alipay.sofa.rpc.test.warmup;

import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 * @version $Id: warmUpTest.java, v 0.1 2018年04月23日 上午10:20 LiWei.Liangen Exp $
 */
public class WarmUpTest {


    @After
    public void after() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Test
    public void testWarmUp() throws InterruptedException {

        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
                .setPort(22222)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(22222))
                .setServer(serverConfig)
                .setRegistry(registryConfig)
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_TIME, "5000")
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT,"100000")
                .setWeight(0);
        providerConfig.export();

        // 记录start时间
        long startTime = System.currentTimeMillis();


        ServerConfig serverConfig2 = new ServerConfig()
                .setPort(22111)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig2 = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(22111))
                .setServer(serverConfig2)
                .setRegistry(registryConfig)
                .setWeight(1);
        providerConfig2.export();


        ConsumerConfig<WarmUpService> consumerConfig = new ConsumerConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRegistry(registryConfig)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        WarmUpService warmUpService = consumerConfig.refer();

        // 5s 之前 流量全部到22222

        while (true){
            Assert.assertEquals(22222, warmUpService.getPort());
            if(System.currentTimeMillis() - startTime >= 4000){
                break;
            }
        }

        Thread.sleep(1000);

        // 5s 之后 流量全部到22111
        for(int i = 0 ; i < 100 ; i++){
            Assert.assertEquals(22111, warmUpService.getPort());
        }


    }

    @Test
    public void testNoWarmUpTime() throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
                .setPort(11222)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(11222))
                .setServer(serverConfig)
                .setRegistry(registryConfig)
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT,"100000")
                .setWeight(0);
        providerConfig.export();

        // 记录start时间
        long startTime = System.currentTimeMillis();


        ServerConfig serverConfig2 = new ServerConfig()
                .setPort(11333)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig2 = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(11333))
                .setServer(serverConfig2)
                .setRegistry(registryConfig)
                .setWeight(1);
        providerConfig2.export();


        ConsumerConfig<WarmUpService> consumerConfig = new ConsumerConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRegistry(registryConfig)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        WarmUpService warmUpService = consumerConfig.refer();



        // 5s 之前 流量全部到11333

        while (true){
            Assert.assertEquals(11333, warmUpService.getPort());
            if(System.currentTimeMillis() - startTime >= 4000){
                break;
            }
        }

        Thread.sleep(1000);

        // 5s 之后 流量还是到11333
        for(int i = 0 ; i < 100 ; i++){
            Assert.assertEquals(11333, warmUpService.getPort());
        }
    }


    @Test
    public void testNoWarmUpWeight() throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
                .setPort(11666)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(11666))
                .setServer(serverConfig)
                .setRegistry(registryConfig)
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_TIME, "5000")
                .setWeight(0);
        providerConfig.export();

        // 记录start时间
        long startTime = System.currentTimeMillis();


        ServerConfig serverConfig2 = new ServerConfig()
                .setPort(11777)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<WarmUpService> providerConfig2 = new ProviderConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRef(new WarmUpServiceImpl(11777))
                .setServer(serverConfig2)
                .setRegistry(registryConfig)
                .setWeight(1);
        providerConfig2.export();


        ConsumerConfig<WarmUpService> consumerConfig = new ConsumerConfig<WarmUpService>()
                .setInterfaceId(WarmUpService.class.getName())
                .setRegistry(registryConfig)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        WarmUpService warmUpService = consumerConfig.refer();



        // 5s 之前 流量全部到11777

        while (true){
            Assert.assertEquals(11777, warmUpService.getPort());
            if(System.currentTimeMillis() - startTime >= 4000){
                break;
            }
        }

        Thread.sleep(1000);

        // 5s 之后 流量还是到11777
        for(int i = 0 ; i < 100 ; i++){
            Assert.assertEquals(11777, warmUpService.getPort());
        }
    }
}