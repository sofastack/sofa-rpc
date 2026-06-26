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
        DubboSingleton.SERVER_MAP.clear();

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

    /**
     * buildUrls should use the resolved virtual address instead of the bound host and port.
     */
    @Test
    public void test_build_urls_use_virtual_host_and_port() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setContextPath("/")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);
        dubboProviderBootstrap.getProviderConfig().setServer(serverConfig);
        dubboProviderBootstrap.exported = true;

        String url = dubboProviderBootstrap.buildUrls().get(0).toString();
        Assert.assertTrue(url.startsWith("dubbo://10.0.0.1:80/" + DemoService.class.getName()));
        Assert.assertTrue(url.contains("?uniqueId="));
    }

    /**
     * buildUrls should fall back to the bound host and port when no virtual address is configured.
     */
    @Test
    public void test_build_urls_fallback_to_host_and_port() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("dubbo")
            .setContextPath("/")
            .setHost("127.0.0.1")
            .setPort(12200);
        dubboProviderBootstrap.getProviderConfig().setServer(serverConfig);
        dubboProviderBootstrap.exported = true;

        String url = dubboProviderBootstrap.buildUrls().get(0).toString();
        Assert.assertTrue(url.startsWith("dubbo://127.0.0.1:12200/" + DemoService.class.getName()));
        Assert.assertTrue(url.contains("?uniqueId="));
    }

    /**
     * Different virtual addresses should not reuse the same cached ProtocolConfig for the same bound address.
     */
    @Test
    public void test_protocol_config_cache_key_should_distinguish_virtual_address() {
        ServerConfig serverConfig1 = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);
        ServerConfig serverConfig2 = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.2")
            .setVirtualPort(81);

        ProtocolConfig protocolConfig1 = dubboProviderBootstrap.getOrCreateProtocolConfig(serverConfig1);
        ProtocolConfig protocolConfig2 = dubboProviderBootstrap.getOrCreateProtocolConfig(serverConfig2);

        Assert.assertNotSame(protocolConfig1, protocolConfig2);
        Assert.assertEquals("10.0.0.1", protocolConfig1.getHost());
        Assert.assertEquals(Integer.valueOf(80), protocolConfig1.getPort());
        Assert.assertEquals("10.0.0.2", protocolConfig2.getHost());
        Assert.assertEquals(Integer.valueOf(81), protocolConfig2.getPort());
    }

    /**
     * Same resolved virtual address should reuse the cached ProtocolConfig even if different ServerConfig instances
     * share the same bound address.
     */
    @Test
    public void test_protocol_config_cache_key_should_reuse_same_virtual_address() {
        ServerConfig serverConfig1 = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);
        ServerConfig serverConfig2 = new ServerConfig()
            .setProtocol("dubbo")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);

        ProtocolConfig protocolConfig1 = dubboProviderBootstrap.getOrCreateProtocolConfig(serverConfig1);
        ProtocolConfig protocolConfig2 = dubboProviderBootstrap.getOrCreateProtocolConfig(serverConfig2);

        Assert.assertSame(protocolConfig1, protocolConfig2);
        Assert.assertEquals("10.0.0.1", protocolConfig1.getHost());
        Assert.assertEquals(Integer.valueOf(80), protocolConfig1.getPort());
    }
}
