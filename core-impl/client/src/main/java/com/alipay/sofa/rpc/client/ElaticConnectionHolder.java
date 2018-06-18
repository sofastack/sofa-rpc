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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import com.alipay.sofa.rpc.transport.ClientTransportFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcOptions.CONCUMER_ELATICCONNECT_SIZE;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_ELATICCONNECT_PRECENT;

/**
 * 弹性长连接，可按百分比配置以及按个数配置
 *
 * @author <a href=mailto:liangyuanpengem@163.com>LiangYuanPeng</a>
 */
@Extension("elatic")
public class ElaticConnectionHolder extends AllConnectConnectionHolder {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER               = LoggerFactory.getLogger(ElaticConnectionHolder.class);

    /**
     * 弹性连接，初始化连接百分比数
     */
    protected int               elaticConnectPrecent = getIntValue(CONSUMER_ELATICCONNECT_PRECENT);

    /**
     * 弹性连接，初始化连接数
     */
    protected int               elaticConnectSize    = getIntValue(CONCUMER_ELATICCONNECT_SIZE);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected ElaticConnectionHolder(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
        this.consumerConfig = consumerBootstrap.getConsumerConfig();
    }

    @Override
    protected void addNode(List<ProviderInfo> providerInfoList) {
        final String interfaceId = consumerConfig.getInterfaceId();
        int providerSize = providerInfoList.size();
        final String appName = consumerConfig.getAppName();
        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "Add provider of {}, size is : {}", interfaceId, providerSize);
        }
        if (providerSize > 0) {

            int minSynConnectSize = 0;
            //可自定义初始化连接的百分比数以及固定最小数
            //计算初始化连接最少数,优先使用初始化最小数属性进行计算,百分比属性默认为0
            if (elaticConnectPrecent > 0) {
                double precent = elaticConnectPrecent >= 100 ? 1 : elaticConnectPrecent * 0.01;
                minSynConnectSize = ((Double) (providerInfoList.size() * precent)).intValue();
            } else {
                minSynConnectSize = elaticConnectSize;
            }

            // 多线程建立连接
            int threads = Math.min(10, minSynConnectSize); // 最大10个
            final CountDownLatch latch = new CountDownLatch(minSynConnectSize);

            int connectTimeout = consumerConfig.getConnectTimeout();

            ThreadPoolExecutor initPool = new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(minSynConnectSize),
                new NamedThreadFactory("CLI-CONN-" + interfaceId, true));

            NamedThreadFactory namedThreadFactory = new NamedThreadFactory("CLI--ASYN-CONN-", true);
            // 第一次同步建立连接的连接数
            int synInitConnectProviderSize = 0;
            for (final ProviderInfo providerInfo : providerInfoList) {
                final ClientTransportConfig config = providerToClientConfig(providerInfo);
                if (synInitConnectProviderSize >= minSynConnectSize) {
                    break;
                }
                synInitConnectProviderSize++;
                initClientRunnable(initPool, latch, providerInfo);
            }

            try {
                int totalTimeout = ((synInitConnectProviderSize % threads == 0) ? (synInitConnectProviderSize / threads)
                    : ((synInitConnectProviderSize /
                    threads) + 1)) * connectTimeout + 500;
                latch.await(totalTimeout, TimeUnit.MILLISECONDS); // 一直等到子线程都结束
            } catch (InterruptedException e) {
                LOGGER.errorWithApp(appName, "Exception when add provider", e);
            } finally {
                initPool.shutdown(); // 关闭线程池
            }

            final List<ProviderInfo> asynConnectProviderInfoList = providerInfoList.subList(synInitConnectProviderSize,
                providerInfoList.size());

            if (!asynConnectProviderInfoList.isEmpty()) {
                LOGGER.debug("asynConnectProviderInfoListSize:{}", asynConnectProviderInfoList.size());
                final ExecutorService executorService = Executors.newFixedThreadPool(1);
                final List<FutureTask> futureTaskList = new ArrayList<FutureTask>();

                namedThreadFactory.newThread(new Runnable() {
                    private FutureTask<String> futureTask;

                    @Override
                    public void run() {
                        ExecutorService executorService = Executors.newFixedThreadPool(1);
                        for (final ProviderInfo providerInfo : asynConnectProviderInfoList) {
                            final ClientTransportConfig config = providerToClientConfig(providerInfo);

                            futureTask = new FutureTask<String>(new Callable<String>() {// 需要的数据类型是String，使用泛型实现！
                                    @Override
                                    public String call() throws Exception {
                                        ClientTransport transport = ClientTransportFactory.getClientTransport(config);
                                        if (consumerConfig.isLazy()) {
                                            uninitializedConnections.put(providerInfo, transport);
                                        } else {
                                            initClientTransport(interfaceId, providerInfo, transport);
                                        }
                                        return providerInfo.getHost() + ":" + providerInfo.getPort();
                                    }
                                });
                            futureTaskList.add(futureTask);
                            executorService.submit(futureTask);
                        }
                    }
                }).run();

            }
        }
    }
}