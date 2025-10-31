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
package com.alipay.sofa.rpc.test.deadline;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.alipay.sofa.rpc.common.RpcOptions.CONFIG_KEY_DEADLINE_ENABLE;

/**
 * Deadline 机制的调用链集成测试抽象基类
 * 调用链: Client -> ServiceA(3s) -> ServiceB(5s) -> ServiceC(5s)
 */
public abstract class AbstractDeadlineChainTest extends ActivelyDestroyTest {
    private final AtomicBoolean isServiceCStarted = new AtomicBoolean(false);

    // 服务接口定义
    public interface ServiceA {
        String processA(String message);
    }

    public interface ServiceB {
        String processB(String message);
    }

    public interface ServiceC {
        String processC(String message);
    }

    // ServiceC 实现 - 最底层服务，模拟5秒处理时间
    public class ServiceCImpl implements ServiceC {
        private volatile int processTime;

        public ServiceCImpl(int processTime) {
            this.processTime = processTime;
        }

        @Override
        public String processC(String message) {
            try {
                isServiceCStarted.set(true); // 标记ServiceC已开始处理，移到开始时设置
                Thread.sleep(processTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ServiceC interrupted", e);
            }
            return "ServiceC-" + message;
        }
    }

    // ServiceB 实现 - 中间层服务，模拟5秒处理时间并调用ServiceC
    public class ServiceBImpl implements ServiceB {
        private final int processTime;
        private ServiceC  serviceC;

        public ServiceBImpl(int processTime) {
            this.processTime = processTime;
        }

        public void setServiceC(ServiceC serviceC) {
            this.serviceC = serviceC;
        }

        @Override
        public String processB(String message) {
            try {
                Thread.sleep(processTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ServiceB interrupted", e);
            }
            // 调用下游服务C
            String resultC = serviceC.processC(message);
            return "ServiceB-" + resultC;
        }
    }

    // ServiceA 实现 - 上层服务，模拟3秒处理时间并调用ServiceB
    public class ServiceAImpl implements ServiceA {
        private final int processTime;
        private ServiceB  serviceB;

        public ServiceAImpl(int processTime) {
            this.processTime = processTime;
        }

        public void setServiceB(ServiceB serviceB) {
            this.serviceB = serviceB;
        }

        @Override
        public String processA(String message) {
            try {
                Thread.sleep(processTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ServiceA interrupted", e);
            }
            // 调用下游服务B
            String resultB = serviceB.processB(message);
            return "ServiceA-" + resultB;
        }
    }

    protected abstract String getProtocolType();

    protected abstract int getBasePort();

    protected void configureProvider(ProviderConfig<?> providerConfig, ServerConfig serverConfig,
                                     ApplicationConfig appConfig) {
    }

    protected void configureConsumer(ConsumerConfig<?> consumerConfig, String protocol, String url,
                                     ApplicationConfig appConfig) {
    }

    /**
     * 执行Deadline调用链测试
     */
    protected void doTestDeadlineChain() throws InterruptedException {
        int basePort = getBasePort();
        String protocol = getProtocolType();

        // 声明需要清理的资源
        ProviderConfig<ServiceC> providerC = null;
        ProviderConfig<ServiceB> providerB = null;
        ProviderConfig<ServiceA> providerA = null;
        ConsumerConfig<ServiceC> consumerConfigC = null;
        ConsumerConfig<ServiceB> consumerConfigB = null;

        try {
            // 配置ServiceC
            ServerConfig serverConfigC = new ServerConfig()
                .setPort(basePort)
                .setProtocol(protocol)
                .setDaemon(true);

            providerC = new ProviderConfig<ServiceC>()
                .setInterfaceId(ServiceC.class.getName())
                .setRef(new ServiceCImpl(5000)) // 5秒处理时间
                .setServer(serverConfigC)
                .setApplication(new ApplicationConfig().setAppName("serviceC"))
                .setRegister(false);

            // 调用协议特定配置
            configureProvider(providerC, serverConfigC, new ApplicationConfig().setAppName("serviceC"));
            providerC.export();

            // 配置ServiceB
            ServerConfig serverConfigB = new ServerConfig()
                .setPort(basePort + 1)
                .setProtocol(protocol)
                .setDaemon(true);

            ServiceBImpl serviceBImpl = new ServiceBImpl(5000); // 5秒处理时间

            // ServiceB调用ServiceC的客户端配置
            consumerConfigC = new ConsumerConfig<ServiceC>()
                .setInterfaceId(ServiceC.class.getName())
                .setTimeout(30000)
                .setApplication(new ApplicationConfig().setAppName("serviceB"));

            // 调用协议特定配置
            String urlC = protocol + "://127.0.0.1:" + basePort;
            configureConsumer(consumerConfigC, protocol, urlC, new ApplicationConfig().setAppName("serviceB"));
            if (consumerConfigC.getDirectUrl() == null) {
                consumerConfigC.setDirectUrl(urlC);
            }

            ServiceC serviceCProxy = consumerConfigC.refer();
            serviceBImpl.setServiceC(serviceCProxy);

            providerB = new ProviderConfig<ServiceB>()
                .setInterfaceId(ServiceB.class.getName())
                .setRef(serviceBImpl)
                .setServer(serverConfigB)
                .setApplication(new ApplicationConfig().setAppName("serviceB"))
                .setRegister(false);

            // 调用协议特定配置
            configureProvider(providerB, serverConfigB, new ApplicationConfig().setAppName("serviceB"));
            providerB.export();

            // 配置ServiceA
            ServerConfig serverConfigA = new ServerConfig()
                .setPort(basePort + 2)
                .setProtocol(protocol)
                .setDaemon(true);

            ServiceAImpl serviceAImpl = new ServiceAImpl(3000); // 3秒处理时间

            // ServiceA调用ServiceB的客户端配置
            consumerConfigB = new ConsumerConfig<ServiceB>()
                .setInterfaceId(ServiceB.class.getName())
                .setTimeout(30000)
                .setApplication(new ApplicationConfig().setAppName("serviceA"));

            // 调用协议特定配置
            String urlB = protocol + "://127.0.0.1:" + (basePort + 1);
            configureConsumer(consumerConfigB, protocol, urlB, new ApplicationConfig().setAppName("serviceA"));
            if (consumerConfigB.getDirectUrl() == null) {
                consumerConfigB.setDirectUrl(urlB);
            }

            ServiceB serviceBProxy = consumerConfigB.refer();
            serviceAImpl.setServiceB(serviceBProxy);

            providerA = new ProviderConfig<ServiceA>()
                .setInterfaceId(ServiceA.class.getName())
                .setRef(serviceAImpl)
                .setServer(serverConfigA)
                .setApplication(new ApplicationConfig().setAppName("serviceA"))
                .setRegister(false);

            // 调用协议特定配置
            configureProvider(providerA, serverConfigA, new ApplicationConfig().setAppName("serviceA"));
            providerA.export();

            Thread.sleep(1000); // 等待服务启动

            testDeadlineTimeout(protocol, basePort + 2);

        } finally {
            // 清理资源 - 按创建的逆序清理
            if (providerA != null) {
                try {
                    providerA.unExport();
                } catch (Exception e) {
                    // 记录异常但不抛出，确保其他资源能继续清理
                    System.err.println("Failed to unexport providerA: " + e.getMessage());
                }
            }

            if (providerB != null) {
                try {
                    providerB.unExport();
                } catch (Exception e) {
                    System.err.println("Failed to unexport providerB: " + e.getMessage());
                }
            }

            if (providerC != null) {
                try {
                    providerC.unExport();
                } catch (Exception e) {
                    System.err.println("Failed to unexport providerC: " + e.getMessage());
                }
            }

            // 清理消费者配置
            if (consumerConfigB != null) {
                try {
                    consumerConfigB.unRefer();
                } catch (Exception e) {
                    System.err.println("Failed to unrefer consumerConfigB: " + e.getMessage());
                }
            }

            if (consumerConfigC != null) {
                try {
                    consumerConfigC.unRefer();
                } catch (Exception e) {
                    System.err.println("Failed to unrefer consumerConfigC: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 测试deadline超时
     */
    private void testDeadlineTimeout(String protocol, int port) throws InterruptedException {
        ConsumerConfig<ServiceA> consumerConfig = new ConsumerConfig<ServiceA>()
            .setInterfaceId(ServiceA.class.getName())
            .setParameter(CONFIG_KEY_DEADLINE_ENABLE, "true")
            .setTimeout(6000)
            .setRepeatedReferLimit(-1) // 允许重复引用
            .setApplication(new ApplicationConfig().setAppName("client2"));

        // 调用协议特定配置
        String url = protocol + "://127.0.0.1:" + port;
        configureConsumer(consumerConfig, protocol, url, new ApplicationConfig().setAppName("client2"));
        if (consumerConfig.getDirectUrl() == null) {
            consumerConfig.setDirectUrl(url);
        }

        isServiceCStarted.set(false);
        ServiceA serviceA = consumerConfig.refer();

        boolean error = false;
        try {
            serviceA.processA("test-message");
            Assert.fail("Should throw timeout exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaTimeOutException);
            Thread.sleep(9000);
            Assert.assertFalse(isServiceCStarted.get());
            error = true;
        }
        Assert.assertTrue(error);
    }
}
