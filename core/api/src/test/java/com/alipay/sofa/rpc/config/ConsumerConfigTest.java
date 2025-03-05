package com.alipay.sofa.rpc.config;

import com.alipay.sofa.rpc.invoke.Invoker;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Even
 * @date 2025/3/4 21:36
 */
public class ConsumerConfigTest {

    @Test
    public void testMethodTimeout() {
        ConsumerConfig<Invoker> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setTimeout(4000);
        consumerConfig.setInterfaceId(Invoker.class.getName());
        consumerConfig.getConfigValueCache(true);
        Assert.assertEquals(4000, consumerConfig.getMethodTimeout("invoke"));

        List<MethodConfig> methodConfigs = new ArrayList<>();
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("invoke");
        methodConfigs.add(methodConfig);
        consumerConfig.setMethods(methodConfigs);
        consumerConfig.getConfigValueCache(true);
        Assert.assertEquals(4000, consumerConfig.getMethodTimeout("invoke"));

        methodConfig.setTimeout(5000);
        consumerConfig.getConfigValueCache(true);
        Assert.assertEquals(5000, consumerConfig.getMethodTimeout("invoke"));

        methodConfig.setTimeout(-1);
        consumerConfig.getConfigValueCache(true);
        Assert.assertEquals(-1, consumerConfig.getMethodTimeout("invoke"));
    }

}