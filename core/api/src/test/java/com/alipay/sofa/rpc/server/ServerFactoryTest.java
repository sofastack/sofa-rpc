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
package com.alipay.sofa.rpc.server;

import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerFactoryTest {
    @Test
    public void getServer() {
        ServerConfig serverConfig = new ServerConfig().setProtocol("test").setPort(1234);
        Server server = ServerFactory.getServer(serverConfig);
        Assert.assertNotNull(server);

        boolean error = false;
        try {
            serverConfig = new ServerConfig().setProtocol("test1").setPort(2345);
            ServerFactory.getServer(serverConfig);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Assert.assertTrue(ServerFactory.getServers().size() > 0);

        ServerFactory.destroyAll();
    }

    @Test
    public void destroyServer() {
        ServerConfig serverConfig = new ServerConfig().setProtocol("test").setPort(1234);
        Server server = serverConfig.buildIfAbsent();
        Assert.assertNotNull(server);
        Assert.assertEquals(1, ServerFactory.getServers().size());
        serverConfig.destroy();
        Assert.assertEquals(0, ServerFactory.getServers().size());
        Assert.assertNull(serverConfig.getServer());
    }
}