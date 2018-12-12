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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.filter.AutoActive;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * Hystrix instrumentation
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
@Extension("hystrix")
@AutoActive(consumerSide = true)
public class HystrixFilter extends Filter {

    private final static Logger LOGGER = LoggerFactory.getLogger(HystrixFilter.class);

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        AbstractInterfaceConfig config = invoker.getConfig();
        // 只支持 consumer 侧
        if (!(config instanceof ConsumerConfig)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warnWithApp(config.getAppName(), "HystrixFilter is not allowed on provider, interfaceId: {}",
                    config.getInterfaceId());
            }
            return false;
        }
        String consumerHystrixEnabled = config.getParameter(HystrixConstants.SOFA_HYSTRIX_ENABLED);
        if (StringUtils.isNotBlank(consumerHystrixEnabled)) {
            return Boolean.valueOf(consumerHystrixEnabled);
        }
        return RpcConfigs.getOrDefaultValue(HystrixConstants.SOFA_HYSTRIX_ENABLED, false);
    }

    @Override
    public SofaResponse invoke(final FilterInvoker invoker, final SofaRequest request) throws SofaRpcException {
        if (RpcConstants.INVOKER_TYPE_SYNC.equals(request.getInvokeType())) {
            return new SofaHystrixCommand(invoker, request).execute();
        }
        else if (RpcConstants.INVOKER_TYPE_FUTURE.equals(request.getInvokeType())) {
            final SofaHystrixObservableCommand command = new SofaHystrixObservableCommand(invoker, request);
            if (command.isCircuitBreakerOpen()) {
                // 直接进行 fallback，不进行真实调用
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warnWithApp(invoker.getConfig().getAppName(), "Circuit Breaker is Opened, interfaceId: {}",
                        invoker.getConfig().getInterfaceId());
                }
            }
            return command.execute();
        }
        return invoker.invoke(request);
    }
}
