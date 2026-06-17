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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltServerTest {
    @Test
    public void start() throws Exception {
        String host = "127.0.0.1";
        int port = 17701;
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        serverConfig.setPort(port);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);

        BoltServer server = new BoltServer();
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        // start use bound port will throw exception
        ServerConfig serverConfig2 = new ServerConfig();
        serverConfig2.setBoundHost(host);
        serverConfig2.setPort(port);
        serverConfig2.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        BoltServer server2 = new BoltServer();
        server2.init(serverConfig2);
        boolean error = false;
        try {
            server2.start();
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        server.stop();
        Assert.assertFalse(server.started);
        waitUntilNotTelnetAble(host, port);

        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        server.stop();
        Assert.assertFalse(server.started);
        waitUntilNotTelnetAble(host, port);

        server.destroy();
    }

    /**
     * Verify that when serverConfig is configured with port 0 (OS-assigned "any" port),
     * BoltServer writes the real bound port back into ServerConfig#actualPort, and that
     * port is reachable.
     */
    @Test
    public void testRandomPort() throws Exception {
        String host = "127.0.0.1";
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        // Use OS-assigned ephemeral port. -1 (SOFA "random") is also acceptable, but bolt
        // RpcServer accepts 0 directly via ServerSocket, so we test 0 here which exercises
        // the new isRandomOrAnyPort branch.
        serverConfig.setPort(0);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);

        BoltServer server = new BoltServer();
        server.init(serverConfig);
        try {
            server.start();
            Assert.assertTrue(server.started);

            // Configured port stays 0 (must not be polluted, otherwise ServerFactory cache
            // and ServerConfig.equals/hashCode would break for callers that constructed the
            // config with port=0).
            Assert.assertEquals(0, serverConfig.getPort());

            // Actual port must be a real OS-assigned port (>0) and reachable.
            int actualPort = serverConfig.getActualPort();
            Assert.assertTrue("actualPort should be > 0 after bind, got: " + actualPort,
                actualPort > 0);
            Assert.assertTrue("server should be reachable on actual port " + actualPort,
                NetUtils.canTelnet(host, actualPort, 1000));

            server.stop();
            Assert.assertFalse(server.started);
            waitUntilNotTelnetAble(host, actualPort);
        } finally {
            server.destroy();
        }
    }

    /**
     * Poll-based wait to replace Thread.sleep, avoiding flakiness on slow CI.
     * Returns as soon as the (host, port) is no longer reachable, or after 5s timeout.
     */
    private static void waitUntilNotTelnetAble(String host, int port) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            if (!NetUtils.canTelnet(host, port, 200)) {
                return;
            }
            Thread.sleep(100);
        }
        Assert.assertFalse("port " + port + " should not be reachable after stop",
            NetUtils.canTelnet(host, port, 1000));
    }

    @Test
    public void threadPoolDestroyTest() {
        String host = "127.0.0.1";
        int port = 17702;
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        serverConfig.setPort(port);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);

        BoltServer server = new BoltServer();
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        server.destroy();
    }

    @Test
    public void testDeprecatedInitThreadExecutor() {
        BoltServer server = new BoltServer();
        server.initThreadPool(new ServerConfig());
        server.destroy();
    }

}