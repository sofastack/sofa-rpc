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
package com.alipay.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * Consumer bootstrap for grpc 
 *
 * @author <a href=mailto:luanyanqiang@dibgroup.cn>Luan Yanqiang</a>
 */
@Extension("grpc")
public class GrpcConsumerBootstrap<T> extends ConsumerBootstrap<T> {

    /**
     * 代理实现类
     */
    protected transient volatile T proxyIns;

    private final ManagedChannel   channel;

    String                         host;
    int                            port;
    private final static Logger    LOGGER = LoggerFactory.getLogger(GrpcConsumerBootstrap.class);

    /**
     * 构造函数
     
     * @param consumerConfig 服务消费者配置
     */
    protected GrpcConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        super(consumerConfig);
        try {
            URL url = new URL(consumerConfig.getDirectUrl());
            host = url.getHost();
            port = url.getPort();
        } catch (Exception e) {
            //TODO: handle exception
            LOGGER.error("illegal direct url: {}", consumerConfig.getDirectUrl());
        }
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    /**
     * Refer t.
     *
     * @return the t
     * @throws SofaRpcRuntimeException the init error exception
     */
    @Override
    public synchronized T refer() {
        if (proxyIns != null) {
            return proxyIns;
        }

        try {
            Method newBlockingChannel = Class.forName(consumerConfig.getInterfaceId()).getDeclaredMethod(
                "newBlockingStub", Channel.class);
            newBlockingChannel.setAccessible(true);
            proxyIns = (T) newBlockingChannel.invoke(null, channel);

        } catch (ClassNotFoundException e) {
            LOGGER.error("ClassNotFoundException");

        } catch (IllegalAccessException e) {
            LOGGER.error("IllegalAccessException");

        } catch (NoSuchMethodException e) {
            LOGGER.error("NoSuchMethodException");

        } catch (InvocationTargetException e) {
            LOGGER.error("InvocationTargetException");

        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException");

        } finally {

        }

        return proxyIns;
    }

    /*
        /**
         * unRefer void.
         */
    @Override
    public synchronized void unRefer() {
        if (proxyIns == null) {
            return;
        }

        // Set to null is sufficient, since GPRC stub doesn't need to be closed.
        proxyIns = null;
    }

    @Override
    public Cluster getCluster() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public List<ProviderGroup> subscribe() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean isSubscribed() {
        return proxyIns != null;
    }

    /**
     * 得到实现代理类
     *
     * @return 实现代理类 proxy ins
     */
    @Override
    public T getProxyIns() {
        return proxyIns;
    }
}
