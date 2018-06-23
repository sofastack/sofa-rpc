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
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IntegrationServerFactoryTest extends ActivelyDestroyTest {

    @Test
    public void getServer() throws Exception {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(new ServerConfig().getHost(), 11234));

            boolean error = false;
            try {
                ServerConfig serverConfig1 = new ServerConfig().setProtocol("bolt").setPort(11234);
                ServerFactory.getServer(serverConfig1).start();
            } catch (Exception e) {
                error = true;
            }
            Assert.assertTrue(error);
            ServerFactory.destroyAll();

            error = false;
            try {
                ServerConfig serverConfig2 = new ServerConfig().setProtocol("bolt").setPort(11234)
                    .setAdaptivePort(true);
                ServerFactory.getServer(serverConfig2).start();
                Assert.assertEquals(11235, serverConfig2.getPort());
            } catch (Exception e) {
                error = true;
            }
            Assert.assertFalse(error);
            ServerFactory.destroyAll();
        } finally {
            if (ss != null) {
                ss.close();
            }
        }
    }
}