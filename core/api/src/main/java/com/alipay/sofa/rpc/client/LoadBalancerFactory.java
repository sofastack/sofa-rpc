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
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * Factory of load balancer
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class LoadBalancerFactory {

    /**
     * 根据名字得到负载均衡器
     *
     * @param consumerBootstrap 服务订阅者配置
     * @return LoadBalancer
     */
    public static LoadBalancer getLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        return getLoadBalancer(consumerBootstrap, consumerBootstrap.getConsumerConfig().getLoadBalancer());
    }

    /**
     * 根据名字和consumer得到负载均衡器
     *
     * @param consumerBootstrap 服务订阅者配置
     * @return LoadBalancer
     */
    public static LoadBalancer getLoadBalancer(ConsumerBootstrap consumerBootstrap, String loadBalancer) {
        try {
            ExtensionClass<LoadBalancer> ext = ExtensionLoaderFactory
                .getExtensionLoader(LoadBalancer.class).getExtensionClass(loadBalancer);
            if (ext == null) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_LOAD_BALANCER, loadBalancer));
            }
            return ext.getExtInstance(new Class[] { ConsumerBootstrap.class }, new Object[] { consumerBootstrap });
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_LOAD_BALANCER, loadBalancer), e);
        }
    }
}
