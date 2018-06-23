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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.base.BaseZkTest;
import com.alipay.sofa.rpc.client.AddressHolder;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.test.TestUtils;

import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ZookeeperOverrideObserverTest extends BaseZkTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperOverrideObserverTest.class);

    @Test
    public void testAll() throws Exception {

        try {
            RegistryConfig registryConfig = new RegistryConfig().setProtocol("zookeeper")
                .setAddress("127.0.0.1:2181");
            ZookeeperRegistry registry = (ZookeeperRegistry) RegistryFactory
                .getRegistry(registryConfig);
            registry.start();

            ServerConfig serverConfig = new ServerConfig().setPort(22222)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
            ProviderConfig<OverrideService> providerConfig = new ProviderConfig<OverrideService>()
                .setInterfaceId(OverrideService.class.getName()).setRef(new OverrideServiceImpl(22222))
                .setServer(serverConfig).setRegistry(registryConfig)
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_TIME, "2000")
                .setParameter(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT, "100").setWeight(0);

            ServerConfig serverConfig2 = new ServerConfig().setPort(22111)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
            ProviderConfig<OverrideService> providerConfig2 = new ProviderConfig<OverrideService>()
                .setInterfaceId(OverrideService.class.getName()).setRef(new OverrideServiceImpl(22111))
                .setServer(serverConfig2).setRegistry(registryConfig).setRepeatedExportLimit(-1)
                .setWeight(0);

            providerConfig.export();
            providerConfig2.export();

            ConsumerConfig<OverrideService> consumerConfig = new ConsumerConfig<OverrideService>()
                .setInterfaceId(OverrideService.class.getName()).setRegistry(registryConfig)
                .setTimeout(3333).setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
            OverrideService overrideService = consumerConfig.refer();

            AddressHolder addressHolder = consumerConfig.getConsumerBootstrap().getCluster()
                .getAddressHolder();
            Assert.assertTrue(addressHolder.getAllProviderSize() == 2);

            providerConfig2.unExport();

            Assert.assertTrue(delayGetSize(addressHolder, 1, 100) == 1);

            List<String> path = registry.getZkClient().getChildren()
                .forPath("/sofa-rpc/" + OverrideService.class.getCanonicalName() + "/providers");
            String url = URLDecoder.decode(path.get(0), "UTF-8");
            ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url);

            // 模拟下发一个override
            String override1 = providerInfo.getProtocolType() + "://" + providerInfo.getHost() + ":"
                + providerInfo.getPort() + "?timeout=2345";
            String overridePath1 = "/sofa-rpc/" + OverrideService.class.getCanonicalName() + "/overrides/"
                + URLEncoder.encode(override1, "UTF-8");
            registry.getZkClient().create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT).forPath(overridePath1);
            Assert.assertTrue(delayGetTimeout(consumerConfig, 2345, 100) == 2345);

            // 删除目前没有影响
            registry.getZkClient().delete().forPath(overridePath1);
            Thread.sleep(500);
            Assert.assertTrue(delayGetTimeout(consumerConfig, 2345, 100) == 2345);

            // 恢复到3333
            String override2 = providerInfo.getProtocolType() + "://" + providerInfo.getHost() + ":"
                + providerInfo.getPort() + "?timeout=3333";
            String overridePath2 = "/sofa-rpc/" + OverrideService.class.getCanonicalName() + "/overrides/"
                + URLEncoder.encode(override2, "UTF-8");
            registry.getZkClient().create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT).forPath(overridePath2);
            Assert.assertTrue(delayGetTimeout(consumerConfig, 3333, 100) == 3333);

            // 清除持久化的 path
            registry.getZkClient().delete().forPath(overridePath2);
        } catch (Throwable e) {
            LOGGER.error("ZookeeperOverrideObserver test case failed", e);
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    int delayGetSize(final AddressHolder addressHolder, int expect, int n50ms) {
        return TestUtils.delayGet(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return addressHolder.getAllProviderSize();
            }
        }, expect, 70, n50ms);
    }

    int delayGetTimeout(final ConsumerConfig consumerConfig, int expect, int n50ms) {
        return TestUtils.delayGet(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return consumerConfig.getTimeout();
            }
        }, expect, 70, n50ms);
    }

}
