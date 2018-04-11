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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;

/**
 * 调用统计维度
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class InvocationStatDimension {

    /**
     * One provider of service reference
     */
    private final ProviderInfo   providerInfo;

    /**
     * Config of service reference
     */
    private final ConsumerConfig consumerConfig;

    /**
     * cache value: dimensionKey
     */
    private transient String     dimensionKey;
    /**
     * cache value : originWeight
     */
    private transient Integer    originWeight;

    /**
     * Instantiates a new Invocation stat dimension.
     *
     * @param providerInfo   the provider info
     * @param consumerConfig the consumer config
     */
    public InvocationStatDimension(ProviderInfo providerInfo, ConsumerConfig consumerConfig) {
        this.providerInfo = providerInfo;
        this.consumerConfig = consumerConfig;
    }

    /**
     * Gets consumer config.
     *
     * @return the consumer config
     */
    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * Gets provider info.
     *
     * @return the provider info
     */
    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }

    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return consumerConfig.getAppName();
    }

    /**
     * Gets service.
     *
     * @return the service
     */
    public String getService() {
        return consumerConfig.getInterfaceId();
    }

    /**
     * Gets ip.
     *
     * @return the ip
     */
    public String getIp() {
        return providerInfo.getHost();
    }

    /**
     * Gets origin weight.
     *
     * @return the origin weight
     */
    public int getOriginWeight() {
        if (originWeight == null) {
            if (providerInfo == null) {
                originWeight = RpcConfigs.getIntValue(RpcOptions.PROVIDER_WEIGHT);
            } else {
                originWeight = CommonUtils.parseInt(providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT),
                    RpcConfigs.getIntValue(RpcOptions.PROVIDER_WEIGHT));
            }
        }
        return originWeight;
    }

    /**
     * Gets dimension key.
     *
     * @return the dimension key
     */
    public String getDimensionKey() {
        if (dimensionKey == null) {
            dimensionKey = getAppName() + ":" + getService();
        }
        return dimensionKey;
    }

    @Override
    public String toString() {
        return (consumerConfig != null ? consumerConfig.buildKey() : "") + "#" + providerInfo.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InvocationStatDimension that = (InvocationStatDimension) o;

        if (providerInfo != null ? !providerInfo.equals(that.providerInfo) : that.providerInfo != null) {
            return false;
        }
        return consumerConfig != null ? consumerConfig.equals(that.consumerConfig) : that.consumerConfig == null;
    }

    @Override
    public int hashCode() {
        int result = providerInfo != null ? providerInfo.hashCode() : 0;
        result = 31 * result + (consumerConfig != null ? consumerConfig.hashCode() : 0);
        return result;
    }
}