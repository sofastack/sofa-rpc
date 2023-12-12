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
package com.alipay.sofa.rpc.common.threadpool.extension;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.threadpool.SofaExecutorFactory;
import com.alipay.sofa.rpc.common.threadpool.ThreadPoolConstant;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.server.BusinessPool;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author junyuan
 * @version DefaultThreadPoolFactory.java, v 0.1 2023年12月11日 20:55 junyuan Exp $
 */
@Extension(value = "cached")
public class CachedThreadPoolFactory implements SofaExecutorFactory {
    protected ServerConfig defaultConfig = new ServerConfig();

    @Override
    public Executor createExecutor() {
        return createExecutor(ThreadPoolConstant.DefaultThreadNamePrefix);
    }

    @Override
    public Executor createExecutor(String namePrefix) {
        return createExecutor(namePrefix, defaultConfig);
    }

    @Override
    public Executor createExecutor(String namePrefix, ServerConfig serverConfig) {
        ThreadPoolExecutor executor = BusinessPool.initPool(serverConfig);
        executor.setThreadFactory(new NamedThreadFactory(namePrefix, serverConfig.isDaemon()));
        return executor;
    }
}
