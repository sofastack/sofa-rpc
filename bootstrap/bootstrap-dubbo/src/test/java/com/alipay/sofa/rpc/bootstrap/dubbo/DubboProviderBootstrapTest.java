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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoService;
import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoServiceImpl;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href=mailto:taobaorun@gmail.com>taobaorun</a>
 */
public class DubboProviderBootstrapTest {

    private DubboProviderBootstrap dubboProviderBootstrap;

    @Before
    public void setUp() throws Exception {

        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        ProviderConfig providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setBootstrap("dubbo")
            .setParameter("version", "1.0.1")
            .setRegister(false).setApplication(serverApplacation);

        dubboProviderBootstrap = new DubboProviderBootstrap(providerConfig);
    }

    @Test
    public void test_dubbo_service_version() {
        Assert.assertEquals("1.0.1", dubboProviderBootstrap.getProviderConfig().getParameter("version"));
    }

    /**
     * virtualHost and virtualPort should override the bound host and port together.
     */
    @Test
    public void test_copy_server_fields_use_virtual_host_and_port() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);
        ProtocolConfig protocolConfig = new ProtocolConfig();

        dubboProviderBootstrap.copyServerFields(serverConfig, protocolConfig);

        Assert.assertEquals("10.0.0.1", protocolConfig.getHost());
        Assert.assertEquals(Integer.valueOf(80), protocolConfig.getPort());
    }

    /**
     * Fall back to the original host and port when no virtual address is configured.
     */
    @Test
    public void test_copy_server_fields_fallback_to_host_and_port() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("127.0.0.1")
            .setPort(12200);
        ProtocolConfig protocolConfig = new ProtocolConfig();

        dubboProviderBootstrap.copyServerFields(serverConfig, protocolConfig);

        Assert.assertEquals("127.0.0.1", protocolConfig.getHost());
        Assert.assertEquals(Integer.valueOf(12200), protocolConfig.getPort());
    }

    /**
     * Blank virtualHost should be treated as unset, while virtualPort still takes effect.
     */
    @Test
    public void test_copy_server_fields_fallback_to_host_when_virtual_host_is_blank() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("127.0.0.1")
            .setPort(12200)
            .setVirtualHost("   ")
            .setVirtualPort(80);
        ProtocolConfig protocolConfig = new ProtocolConfig();

        dubboProviderBootstrap.copyServerFields(serverConfig, protocolConfig);

        Assert.assertEquals("127.0.0.1", protocolConfig.getHost());
        Assert.assertEquals(Integer.valueOf(80), protocolConfig.getPort());
    }

    /**
     * If only virtualHost is configured, the original port should still be used.
     */
    @Test
    public void test_copy_server_fields_use_virtual_host_and_origin_port_when_virtual_port_is_null() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1");
        ProtocolConfig protocolConfig = new ProtocolConfig();

        dubboProviderBootstrap.copyServerFields(serverConfig, protocolConfig);

        Assert.assertEquals("10.0.0.1", protocolConfig.getHost());
        Assert.assertEquals(Integer.valueOf(12200), protocolConfig.getPort());
    }

    /**
     * If only virtualPort is configured, the original host should still be used.
     */
    @Test
    public void test_copy_server_fields_use_origin_host_and_virtual_port_when_virtual_host_is_null() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("127.0.0.1")
            .setPort(12200)
            .setVirtualPort(80);
        ProtocolConfig protocolConfig = new ProtocolConfig();

        dubboProviderBootstrap.copyServerFields(serverConfig, protocolConfig);

        Assert.assertEquals("127.0.0.1", protocolConfig.getHost());
        Assert.assertEquals(Integer.valueOf(80), protocolConfig.getPort());
    }
}
