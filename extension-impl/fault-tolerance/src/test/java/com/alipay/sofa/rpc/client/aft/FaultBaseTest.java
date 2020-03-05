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
package com.alipay.sofa.rpc.client.aft;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.AddressHolder;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.aft.bean.FaultHelloService;
import com.alipay.sofa.rpc.client.aft.bean.FaultHelloService2;
import com.alipay.sofa.rpc.client.aft.bean.HelloServiceTimeOutImpl;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.module.FaultToleranceModule;
import com.alipay.sofa.rpc.module.Module;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public abstract class FaultBaseTest {

    protected static final Logger                LOGGER    = LoggerFactory.getLogger(FaultBaseTest.class);

    public static final String                   APP_NAME1 = "testApp";
    public static final String                   APP_NAME2 = "testAnotherApp";

    protected ServerConfig                       serverConfig;
    protected ConsumerConfig<FaultHelloService>  consumerConfigNotUse;
    protected ConsumerConfig<FaultHelloService>  consumerConfig;
    protected ConsumerConfig<FaultHelloService2> consumerConfig2;
    protected ConsumerConfig<FaultHelloService>  consumerConfigAnotherApp;

    protected ProviderConfig<FaultHelloService>  providerConfig;

    @Before
    public void init() {
        // 只有1个线程 执行
        serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12299)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(10).setMaxThreads(20);

        ApplicationConfig providerAconfig = new ApplicationConfig();
        providerAconfig.setAppName("testApp");

        // 发布一个服务，每个请求要执行1秒
        providerConfig = new ProviderConfig<FaultHelloService>()
            .setInterfaceId(FaultHelloService.class.getName())
            .setRef(new HelloServiceTimeOutImpl())
            .setServer(serverConfig)
            .setRegister(false)
            .setApplication(providerAconfig);

        // just for test
        consumerConfigNotUse = new ConsumerConfig<FaultHelloService>()
            .setInterfaceId(FaultHelloService.class.getName())
            .setTimeout(500)
            .setDirectUrl("127.0.0.1:12299")
            .setRegister(false)
            .setUniqueId("xxx")
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppName(APP_NAME1);
        consumerConfig = new ConsumerConfig<FaultHelloService>()
            .setInterfaceId(FaultHelloService.class.getName())
            .setTimeout(500)
            .setDirectUrl("127.0.0.1:12299")
            .setRegister(false)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setApplication(applicationConfig);

        consumerConfig2 = new ConsumerConfig<FaultHelloService2>()
            .setInterfaceId(FaultHelloService2.class.getName())
            .setTimeout(500)
            .setDirectUrl("127.0.0.1:12299")
            .setRegister(false)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setApplication(applicationConfig);

        consumerConfigAnotherApp = new ConsumerConfig<FaultHelloService>()
            .setInterfaceId(FaultHelloService.class.getName())
            .setDirectUrl("127.0.0.1:12299")
            .setTimeout(500)
            .setRegister(true)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setApplication(new ApplicationConfig().setAppName(APP_NAME2));

        FaultToleranceModule module = (FaultToleranceModule) ExtensionLoaderFactory.getExtensionLoader(Module.class)
            .getExtension("fault-tolerance");
        module.getRegulator().init();

    }

    @After
    public void destroy() {
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, null);
        FaultToleranceConfigManager.putAppConfig(APP_NAME2, null);

        InvocationStatFactory.destroy();

        FaultToleranceModule module = (FaultToleranceModule) ExtensionLoaderFactory.getExtensionLoader(Module.class)
            .getExtension("fault-tolerance");
        module.getRegulator().destroy();
    }

    static ProviderInfo getProviderInfoByHost(ConsumerConfig consumerConfig, String host) {
        ConsumerBootstrap consumerBootStrap = consumerConfig.getConsumerBootstrap();
        AddressHolder addressHolder = consumerBootStrap.getCluster().getAddressHolder();

        List<ProviderGroup> providerGroups = addressHolder.getProviderGroups();

        for (ProviderGroup providerGroup : providerGroups) {
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                if (providerInfo.getHost().equals(host)) {
                    return providerInfo;
                }
            }
        }
        return null;
    }

    /**
     * because of weight degrade/recover is async, this get method will delay n*50ms
     * @param providerInfo ProviderInfo
     * @param expect expect weight
     * @param n50ms N * 50ms
     * @return weight
     */
    static int delayGetWeight(final ProviderInfo providerInfo, int expect, int n50ms) {
        return delayGet(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ProviderInfoWeightManager.getWeight(providerInfo);
            }
        }, expect, 70, n50ms);
        // 本来应该是50ms，我们把50改为70。是因为如果测试机器性能太差，间隔太小会等不到数据
    }

    /**
     * because of subscriber is async, this get method will delay 100ms
     * @param invocationStat InvocationStat
     * @param expect expect count
     * @return count 
     */
    static long delayGetCount(final InvocationStat invocationStat, long expect) {
        return delayGet(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return invocationStat.getInvokeCount();
            }
        }, expect, 10, 10);
    }

    static <T> T delayGet(Callable<T> callable, T expect, int period, int times) {
        T result = null;
        int i = 0;
        while (i++ < times) {
            try {
                Thread.sleep(period);//第一个窗口结束
                result = callable.call();
                if (result != null && result.equals(expect)) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}