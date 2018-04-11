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
package com.alipay.sofa.rpc.router;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.AddressHolder;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.Router;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;

import java.util.List;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_ROUTER_RECORD;

/**
 * Router for ip transmit
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitRouter extends Router {

    /**
     * 服务消费者配置
     */
    protected ConsumerBootstrap consumerBootstrap;

    @Override
    public void init(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

    @Override
    public List<ProviderInfo> route(SofaRequest request, List<ProviderInfo> providerInfos) {
        AddressHolder addressHolder = consumerBootstrap.getCluster().getAddressHolder();
        if (addressHolder != null) {
            List<ProviderInfo> current = addressHolder.getProviderInfos(RpcConstants.ADDRESS_DEFAULT_GROUP);
            if (providerInfos != null) {
                providerInfos.addAll(current);
            } else {
                providerInfos = current;
            }
        }
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext context = RpcInternalContext.getContext();
            String record = (String) context.getAttachment(INTERNAL_KEY_ROUTER_RECORD);
            record = record == null ? "transmit" : record + ">transmit";
            context.setAttachment(INTERNAL_KEY_ROUTER_RECORD, record);
        }
        return providerInfos;
    }
}
