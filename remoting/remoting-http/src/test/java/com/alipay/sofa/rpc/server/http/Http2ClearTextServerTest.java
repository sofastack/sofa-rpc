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
package com.alipay.sofa.rpc.server.http;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http2ClearTextServerTest {

    @Test
    public void start() throws InterruptedException {

        String host = "127.0.0.1";
        int port = 17701;
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        serverConfig.setPort(port);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_H2C);

        Http2ClearTextServer server = new Http2ClearTextServer();
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        // start use bound port will throw exception
        ServerConfig serverConfig2 = new ServerConfig();
        serverConfig2.setBoundHost(host);
        serverConfig2.setPort(port);
        serverConfig2.setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        Http2ClearTextServer server2 = new Http2ClearTextServer();
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
        Thread.sleep(1000); // 升级bolt后删除此行
        Assert.assertFalse(NetUtils.canTelnet(host, port, 1000));

        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        server.stop();
        Assert.assertFalse(server.started);
        Thread.sleep(1000); // 升级bolt后删除此行
        Assert.assertFalse(NetUtils.canTelnet(host, port, 1000));

        server.destroy();
    }
}