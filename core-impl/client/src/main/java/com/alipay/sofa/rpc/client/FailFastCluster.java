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
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * 快速失败
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("failfast")
public class FailFastCluster extends AbstractCluster {

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public FailFastCluster(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public SofaResponse doInvoke(SofaRequest request) throws SofaRpcException {
        ProviderInfo providerInfo = select(request);
        try {
            SofaResponse response = filterChain(providerInfo, request);
            if (response != null) {
                return response;
            } else {
                throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                    "Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                        + " on remote server " + providerInfo + ", return null");
            }
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                    + " on remote server: " + providerInfo + ", cause by: "
                    + e.getClass().getName() + ", message is: " + e.getMessage(), e);
        }
    }
}
