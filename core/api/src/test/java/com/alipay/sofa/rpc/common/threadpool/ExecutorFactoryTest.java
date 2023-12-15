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
package com.alipay.sofa.rpc.common.threadpool;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author junyuan
 * @version ExecutorFactoryTest.java, v 0.1 2023年12月15日 10:59 junyuan Exp $
 */
public class ExecutorFactoryTest {

    @Test
    public void testBuildCachedPool() {
        Executor executor = ExtensionLoaderFactory.getExtensionLoader(SofaExecutorFactory.class).getExtension("cached")
            .createExecutor();
        Assert.assertTrue(executor instanceof ThreadPoolExecutor);
        ServerConfig serverConfig = new ServerConfig();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        Assert.assertEquals(threadPoolExecutor.getCorePoolSize(), serverConfig.getCoreThreads());
    }
}
