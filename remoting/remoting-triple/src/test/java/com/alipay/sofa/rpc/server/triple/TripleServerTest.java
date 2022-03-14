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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.Server;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zhaowang
 * @version : TripleServerTest.java, v 0.1 2020年09月08日 3:28 下午 zhaowang Exp $
 */
public class TripleServerTest {

    @Test
    public void testBizThreadPool() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setProtocol("tri");
        serverConfig.setPort(50055);
        serverConfig.buildIfAbsent().start();
        Server server = serverConfig.getServer();
        Assert.assertTrue(server instanceof TripleServer);
        ThreadPoolExecutor bizThreadPool = ((TripleServer) server).getBizThreadPool();
        Assert.assertNotNull(bizThreadPool);
        server.stop();
    }
}