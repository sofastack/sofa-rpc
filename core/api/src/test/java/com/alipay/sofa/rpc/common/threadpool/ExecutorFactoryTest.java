/*
 * Ant Group
 * Copyright (c) 2004-2023 All Rights Reserved.
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
        Executor executor = ExtensionLoaderFactory.getExtensionLoader(SofaExecutorFactory.class).getExtension("cached").createExecutor();
        Assert.assertTrue(executor instanceof ThreadPoolExecutor);
        ServerConfig serverConfig = new ServerConfig();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        Assert.assertEquals(threadPoolExecutor.getCorePoolSize(), serverConfig.getCoreThreads());
    }
}
