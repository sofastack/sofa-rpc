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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;

/**
 * <p>执行真正的调用过程，使用client发送数据给server</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ConsumerInvoker extends FilterInvoker {

    /**
     * The Client.
     */
    private ConsumerBootstrap consumerBootstrap;

    /**
     * Instantiates a new Consumer invoke filter.
     *
     * @param consumerBootstrap 服务器启动着配置
     */
    public ConsumerInvoker(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap.getConsumerConfig());
        this.consumerBootstrap = consumerBootstrap;
    }

    @Override
    public SofaResponse invoke(SofaRequest sofaRequest) throws SofaRpcException {
        // 设置下服务器应用
        ProviderInfo providerInfo = RpcInternalContext.getContext().getProviderInfo();
        String appName = providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_APP_NAME);
        // R3: Record consumer filter execution time
        Long consumerFilterStartTime = (Long) RpcInvokeContext.getContext().get(
            RpcConstants.INTERNAL_KEY_CONSUMER_FILTER_START_TIME_NANO);
        if (consumerFilterStartTime != null) {
            RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_CLIENT_FILTER_TIME_NANO,
                System.nanoTime() - consumerFilterStartTime);
        }

        if (StringUtils.isNotEmpty(appName)) {
            sofaRequest.setTargetAppName(appName);
        }

        // 目前只是通过client发送给服务端
        return consumerBootstrap.getCluster().sendMsg(providerInfo, sofaRequest);
    }

}
