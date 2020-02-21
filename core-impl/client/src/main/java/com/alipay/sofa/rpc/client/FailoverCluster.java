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
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 故障转移，支持重试和指定地址调用
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("failover")
public class FailoverCluster extends AbstractCluster {

    /**
     * slf4j logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(FailoverCluster.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public FailoverCluster(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public SofaResponse doInvoke(SofaRequest request) throws SofaRpcException {
        String methodName = request.getMethodName();
        int retries = consumerConfig.getMethodRetries(methodName);
        int time = 0;
        SofaRpcException throwable = null;// 异常日志
        List<ProviderInfo> invokedProviderInfos = new ArrayList<ProviderInfo>(retries + 1);
        do {

            ProviderInfo providerInfo = select(request, invokedProviderInfos);
            try {
                SofaResponse response = filterChain(providerInfo, request);
                if (response != null) {
                    if (throwable != null) {
                        if (LOGGER.isWarnEnabled(consumerConfig.getAppName())) {
                            LOGGER.warnWithApp(consumerConfig.getAppName(),
                                LogCodes.getLog(LogCodes.WARN_SUCCESS_BY_RETRY,
                                    throwable.getClass() + ":" + throwable.getMessage(),
                                    invokedProviderInfos));
                        }
                    }
                    return response;
                } else {
                    throwable = new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                        "Failed to call " + request.getInterfaceName() + "." + methodName
                            + " on remote server " + providerInfo + ", return null");
                    time++;
                }
            } catch (SofaRpcException e) { // 服务端异常+ 超时异常 才发起rpc异常重试
                if (e.getErrorType() == RpcErrorType.SERVER_BUSY
                    || e.getErrorType() == RpcErrorType.CLIENT_TIMEOUT) {
                    throwable = e;
                    time++;
                } else {
                    throw e;
                }
            } catch (Exception e) { // 其它异常不重试
                throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                    "Failed to call " + request.getInterfaceName() + "." + request.getMethodName()
                        + " on remote server: " + providerInfo + ", cause by unknown exception: "
                        + e.getClass().getName() + ", message is: " + e.getMessage(), e);
            } finally {
                if (RpcInternalContext.isAttachmentEnable()) {
                    RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_INVOKE_TIMES,
                        time + 1); // 重试次数
                }
            }
            invokedProviderInfos.add(providerInfo);
        } while (time <= retries);

        throw throwable;
    }

}
