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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RestServerTest {

    @Test
    public void start() {
        String host = "127.0.0.1";
        int port = 18801;
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        serverConfig.setPort(port);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_REST);

        RestServer server = new RestServer();
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));
        // 重复启动
        server.start();

        server.stop();
        Assert.assertFalse(server.started);
        Assert.assertFalse(NetUtils.canTelnet(host, port, 1000));
        // 重复关闭
        server.stop();

        // 销毁
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));
        server.destroy(null);

        // 销毁
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));
        server.destroy(new Destroyable.DestroyHook() {
            @Override
            public void preDestroy() {

            }

            @Override
            public void postDestroy() {

            }
        });
    }

    @Test
    public void startAllowedOrigins() {
        String host = "127.0.0.1";
        int port = 18801;
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBoundHost(host);
        serverConfig.setPort(port);
        serverConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_REST);
        Map<String, String> map = new HashMap<String, String>();
        final String ip1 = "http://127.0.0.1";
        final String ip2 = "http://127.0.0.2";
        map.put(RpcConstants.ALLOWED_ORIGINS, ip1 + "," + ip2 + ",");
        serverConfig.setParameters(map);
        RestServer server = new RestServer();
        server.init(serverConfig);
        server.start();
        Assert.assertTrue(server.started);
        Assert.assertTrue(NetUtils.canTelnet(host, port, 1000));

        server.stop();

        //主要是判断一下filter加进去了没有

        Set<Object> filter = JAXRSProviderManager.getCustomProviderInstances();
        CorsFilter corsFilter = (CorsFilter) filter.iterator().next();
        Set<String> allows = corsFilter.getAllowedOrigins();
        Assert.assertEquals(2, allows.size());
        Assert.assertTrue(allows.contains(ip1));
        Assert.assertTrue(allows.contains(ip2));
    }
}