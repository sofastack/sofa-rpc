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

import com.alipay.sofa.rpc.bootstrap.DefaultConsumerBootstrap;
import com.alipay.sofa.rpc.client.ClusterFactory;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.ProxyFactory;
import io.grpc.CallOptions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer bootstrap for grpc
 *
 * @author <a href=mailto:yqluan@gmail.com>Yanqiang Oliver Luan (neokidd)</a>
 */
@Extension("grpc")
public class GrpcConsumerBootstrap<T> extends DefaultConsumerBootstrap<T> {

    /**
     * Set by ConsumerConfig.setInterfaceID(), or interface attribution in XML service definition.
     * <p>
     * PLEASE NOTE: For GRPC transport, interfaceID won't refer to a real interface, it should be
     * FULL QUALIFIED name of the concrete CLASS, which is generated from .proto file.
     * e.g. GreeterGrpc.class.getName()
     */
    private final String interfaceID;

    /**
     * 代理实现类
     */
    protected transient volatile T proxyIns;

    private final static Logger LOGGER = LoggerFactory.getLogger(GrpcConsumerBootstrap.class);

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected GrpcConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        super(consumerConfig);
        this.interfaceID = consumerConfig.getInterfaceId();
    }

    /**
     * Refer t.
     *
     * @return the t
     * @throws SofaRpcRuntimeException the init error exception
     */
    @Override
    public synchronized T refer() {
        if (this.proxyIns != null) {
            return (T) this.proxyIns;
        }

        String key = consumerConfig.buildKey();
        String appName = consumerConfig.getAppName();
        // 检查参数
        checkParameters();
        // 提前检查接口类
        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "Refer consumer config : {} with bean id {}", key, consumerConfig.getId());
        }

        // 注意同一interface，同一tags，同一protocol情况
        AtomicInteger cnt = REFERRED_KEYS.get(key); // 计数器
        if (cnt == null) { // 没有发布过
            cnt = CommonUtils.putToConcurrentMap(REFERRED_KEYS, key, new AtomicInteger(0));
        }
        int c = cnt.incrementAndGet();
        int maxProxyCount = consumerConfig.getRepeatedReferLimit();
        if (maxProxyCount > 0) {
            if (c > maxProxyCount) {
                cnt.decrementAndGet();
                // 超过最大数量，直接抛出异常
                throw new SofaRpcRuntimeException("Duplicate consumer config with key " + key
                        + " has been referred more than " + maxProxyCount + " times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!");
            } else if (c > 1) {
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, "Duplicate consumer config with key {} has been referred!"
                            + " Maybe it's wrong config, please check it."
                            + " Ignore this if you did that on purpose!", key);
                }
            }
        }

        try {
            // build cluster
            cluster = ClusterFactory.getCluster(this);
            // build listeners
            consumerConfig.setConfigListener(buildConfigListener(this));
            consumerConfig.setProviderInfoListener(buildProviderInfoListener(this));
            // init cluster
            cluster.init();
            // 构造Invoker对象（执行链）
            proxyInvoker = buildClientProxyInvoker(this);
            // 创建代理类
            proxyIns = (T) ProxyFactory.buildProxy(consumerConfig.getProxy(), consumerConfig.getProxyClass(),
                    proxyInvoker);
        } catch (Exception e) {
            if (cluster != null) {
                cluster.destroy();
                cluster = null;
            }
            consumerConfig.setConfigListener(null);
            consumerConfig.setProviderInfoListener(null);
            cnt.decrementAndGet(); // 发布失败不计数
            if (e instanceof SofaRpcRuntimeException) {
                throw (SofaRpcRuntimeException) e;
            } else {
                throw new SofaRpcRuntimeException("Build consumer proxy error!", e);
            }
        }

        if (consumerConfig.getOnAvailable() != null && cluster != null) {
            cluster.checkStateChange(false); // 状态变化通知监听器
        }
        RpcRuntimeContext.cacheConsumerConfig(this);

        return (T) this.proxyIns;
    }

    /*
        /**
         * unRefer void.
         */
    @Override
    public synchronized void unRefer() {
        if (this.proxyIns == null) {
            return;
        }

        // Do not disconnect. GPRC connection should be closed by connection holders.
        this.proxyIns = null;
    }

    private static CallOptions buildCallOptions() {
        // gRPC Deadline starts counting when it's created, so we need to create and add a new Deadline for each RPC call.
        //        CallOptions callOptions = CallOptions.DEFAULT
        //                .withDeadline(Deadline.after(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), TimeUnit.MILLISECONDS));
        CallOptions callOptions = CallOptions.DEFAULT;
        return callOptions;
    }
}
