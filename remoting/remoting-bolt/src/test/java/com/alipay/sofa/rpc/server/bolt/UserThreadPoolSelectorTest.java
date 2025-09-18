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
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.config.UserThreadPoolManager;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.server.Server;
import com.alipay.sofa.rpc.server.UserThreadPool;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Even
 * @date 2025/7/24 16:42
 */
public class UserThreadPoolSelectorTest {

    private static BoltServer     boltServer;

    private static UserThreadPool interfaceUserThreadPool = new UserThreadPool();

    private static UserThreadPool methodUserThreadPool    = new UserThreadPool();

    @BeforeClass
    public static void beforeClass() {
        boltServer = (BoltServer) ExtensionLoaderFactory.getExtensionLoader(Server.class).getExtension("bolt");
        boltServer.init(new ServerConfig());
        interfaceUserThreadPool.init();
        methodUserThreadPool.init();
        UserThreadPoolManager.registerUserThread("interface:1.0:uniqueId", interfaceUserThreadPool);
        UserThreadPoolManager.registerUserThread("interface:1.0:uniqueId", "hello", methodUserThreadPool);
    }

    @Test
    public void select() {
        UserThreadPoolSelector userThreadPoolSelector = new UserThreadPoolSelector(boltServer.getBizExecutor());
        Map<String, Object> map = new HashMap<>();
        map.put(RemotingConstants.HEAD_SERVICE, "interface:1.0:uniqueId");
        Assert.assertEquals(interfaceUserThreadPool.getUserExecutor(), userThreadPoolSelector.select("com.alipay.sofa.rpc.core.request.SofaRequest", map));
        map.put(RemotingConstants.HEAD_METHOD_NAME, "hello");
        Assert.assertEquals(methodUserThreadPool.getUserExecutor(), userThreadPoolSelector.select("com.alipay.sofa.rpc.core.request.SofaRequest", map));
        map.put(RemotingConstants.HEAD_METHOD_NAME, "message");
        Assert.assertEquals(interfaceUserThreadPool.getUserExecutor(), userThreadPoolSelector.select("com.alipay.sofa.rpc.core.request.SofaRequest", map));
        map.put(RemotingConstants.HEAD_SERVICE, "interface:1.0");
        Assert.assertEquals(boltServer.getBizExecutor(), userThreadPoolSelector.select("com.alipay.sofa.rpc.core.request.SofaRequest", map));
        map.put(RemotingConstants.HEAD_SERVICE, new HashMap<>());
        Assert.assertEquals(boltServer.getBizExecutor(), userThreadPoolSelector.select("com.alipay.sofa.rpc.core.request.SofaRequest", map));
    }

    @Test
    public void customUserThreadPoolSelector() {
        UserProcessor.ExecutorSelector executorSelector = getCustomExecutorSelector(boltServer);
        Map<String, String> map = new HashMap<>();
        Assert.assertEquals(boltServer.getBizExecutor(), executorSelector.select("", map));
        map.put("customThreadPool", "method");
        Assert.assertEquals(methodUserThreadPool.getUserExecutor(), executorSelector.select("", map));
        map.put("customThreadPool", "interface");
        Assert.assertEquals(interfaceUserThreadPool.getUserExecutor(), executorSelector.select("", map));
        map.remove("customThreadPool");
        Assert.assertEquals(boltServer.getBizExecutor(), executorSelector.select("", map));
    }

    private static UserProcessor.ExecutorSelector getCustomExecutorSelector(BoltServer boltServer) {
        BoltServerProcessor boltServerProcessor = boltServer.getBoltServerProcessor();
        boltServerProcessor.setExecutorSelector((requestClass, requestHeader) -> {
            Map<String, String> headerMap = (Map<String, String>) requestHeader;
            if (headerMap.containsKey("customThreadPool")) {
                if (headerMap.get("customThreadPool").equals("method")) {
                    return methodUserThreadPool.getUserExecutor();
                } else {
                    return interfaceUserThreadPool.getUserExecutor();
                }
            }
            return boltServer.getBizExecutor();
        });
        return boltServerProcessor.getExecutorSelector();
    }

    @AfterClass
    public static void afterClass() {
        UserThreadPoolManager.unRegisterUserThread("interface:1.0:uniqueId");
        UserThreadPoolManager.unRegisterUserThread("interface:1.0:uniqueId", "hello");
        interfaceUserThreadPool = null;
        methodUserThreadPool = null;
        boltServer = null;
    }
}