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

import com.alipay.sofa.rpc.common.threadpool.SofaExecutorFactory;
import com.alipay.sofa.common.thread.virtual.SofaVirtualThreadFactory;
import com.alipay.sofa.rpc.common.threadpool.ThreadPoolConstant;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author junyuan
 * @version VirtualThreadPoolFactory.java, v 0.1 2023年12月12日 11:11 junyuan Exp $
 */
@Extension(value = "virtual")
public class VirtualThreadPoolFactory implements SofaExecutorFactory {

    /**
     * 系统全局线程池计数器
     */
    private static final AtomicInteger POOL_COUNT = new AtomicInteger();

    @Override
    public Executor createExecutor(String namePrefix, ServerConfig serverConfig) {
        // virtual thread does not support any configs now
        return SofaVirtualThreadFactory.ofExecutorService(buildNamePrefix(namePrefix));
    }

    /**
     * refine virtual thread name
     * SOFA-originInput-0-VT123
     * @param namePrefix
     * @return
     */
    private String buildNamePrefix(String namePrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append("SOFA-");
        sb.append(namePrefix);
        sb.append("-");
        sb.append(POOL_COUNT.getAndIncrement());
        sb.append("-");
        sb.append(ThreadPoolConstant.TYPE_PREFIX_VIRTUAL_TREAD);
        return sb.toString();
    }
}
