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
package com.alipay.sofa.rpc.server.bolt;

import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.UserThreadPoolManager;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.UserThreadPool;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Even
 * @date 2025/7/22 20:16
 */
public class UserThreadPoolSelector implements UserProcessor.ExecutorSelector {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserThreadPoolSelector.class);

    private final Executor      defaultExecutor;

    public UserThreadPoolSelector(Executor defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public Executor select(String requestClass, Object requestHeader) {
        if (SofaRequest.class.getName().equals(requestClass)
            && requestHeader != null) {
            Map<String, String> headerMap = (Map<String, String>) requestHeader;
            try {
                String service = headerMap.get(RemotingConstants.HEAD_SERVICE);
                if (service == null) {
                    service = headerMap.get(RemotingConstants.HEAD_TARGET_SERVICE);
                }
                if (service != null) {
                    UserThreadPool threadPool;
                    String methodName = headerMap.get(RemotingConstants.HEAD_METHOD_NAME);
                    threadPool = UserThreadPoolManager.getUserThread(service, methodName);
                    if (threadPool != null) {
                        Executor executor = threadPool.getUserExecutor();
                        if (executor != null) {
                            // 存在自定义线程池，且不为空
                            return executor;
                        }
                    }
                    threadPool = UserThreadPoolManager.getUserThread(service);
                    if (threadPool != null) {
                        Executor executor = threadPool.getUserExecutor();
                        if (executor != null) {
                            // 存在自定义线程池，且不为空
                            return executor;
                        }
                    }
                }
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(LogCodes.getLog(LogCodes.WARN_DESERIALIZE_HEADER_ERROR), e);
                }
            }
        }
        return defaultExecutor;

    }
}
