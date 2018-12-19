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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangchengxi
 * Date 2018/12/2
 */
public class CustomizeFilterTest extends ActivelyDestroyTest {

    private static RestService                          filterRestService;

    private static CustomizeTestFilter                  providerFilter;

    private static CustomizeTestFilter                  clientFilter;

    private static ProviderConfig<RestService>          providerConfig;

    private static CustomizeContainerRequestTestFilter  customizeContainerRequestTestFilter;

    private static CustomizeContainerResponseTestFilter customizeContainerResponseTestFilter;

    private static CustomizeClientRequestTestFilter     customizeClientRequestTestFilter;

    private static CustomizeClientResponseTestFilter    customizeClientResponseTestFilter;

    @BeforeClass
    public static void beforeClass() {
        customizeContainerRequestTestFilter = new CustomizeContainerRequestTestFilter();
        customizeContainerResponseTestFilter = new CustomizeContainerResponseTestFilter();
        customizeClientRequestTestFilter = new CustomizeClientRequestTestFilter();
        customizeClientResponseTestFilter = new CustomizeClientResponseTestFilter();

        JAXRSProviderManager.registerCustomProviderInstance(customizeContainerRequestTestFilter);
        JAXRSProviderManager.registerCustomProviderInstance(customizeContainerResponseTestFilter);
        JAXRSProviderManager.registerCustomProviderInstance(customizeClientRequestTestFilter);
        JAXRSProviderManager.registerCustomProviderInstance(customizeClientResponseTestFilter);

        providerFilter = new CustomizeTestFilter();
        List<Filter> providerFilters = new ArrayList<Filter>(2);
        providerFilters.add(providerFilter);

        ServerConfig restServer = new ServerConfig()
            .setPort(8583)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST);

        List<ServerConfig> servers = new ArrayList<ServerConfig>(2);
        servers.add(restServer);
        providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setRegister(false)
            .setServer(servers)
            .setFilterRef(providerFilters);

        providerConfig.export();

        //rest服务
        clientFilter = new CustomizeTestFilter();
        List<Filter> clientFilters = new ArrayList<Filter>(2);
        clientFilters.add(clientFilter);

        ConsumerConfig<RestService> consumerConfigRest = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
            .setDirectUrl("rest://127.0.0.1:8583")
            .setTimeout(1000)
            .setFilterRef(clientFilters)
            .setApplication(new ApplicationConfig().setAppName("TestClientRest"));
        filterRestService = consumerConfigRest.refer();
    }

    @AfterClass
    public static void afterClass() {
        JAXRSProviderManager.removeCustomProviderInstance(customizeContainerRequestTestFilter);
        JAXRSProviderManager.removeCustomProviderInstance(customizeContainerResponseTestFilter);
        JAXRSProviderManager.removeCustomProviderInstance(customizeClientRequestTestFilter);
        JAXRSProviderManager.removeCustomProviderInstance(customizeClientResponseTestFilter);
    }

    @Before
    public void before() {
        CustomizeContainerRequestTestFilter.reset();
        CustomizeContainerResponseTestFilter.reset();
        CustomizeClientRequestTestFilter.reset();
        CustomizeClientResponseTestFilter.reset();
        providerFilter.reset();
        clientFilter.reset();
    }

    @Test
    public void testFilterInvoked() {
        Assert.assertFalse(CustomizeContainerRequestTestFilter.isInvoked());
        Assert.assertFalse(CustomizeContainerResponseTestFilter.isInvoked());
        Assert.assertFalse(CustomizeClientRequestTestFilter.isInvoked());
        Assert.assertFalse(CustomizeClientResponseTestFilter.isInvoked());
        Assert.assertFalse(providerFilter.isInvoked());
        Assert.assertFalse(clientFilter.isInvoked());

        filterRestService.get("test");

        Assert.assertTrue(CustomizeContainerRequestTestFilter.isInvoked());
        Assert.assertTrue(CustomizeContainerResponseTestFilter.isInvoked());
        Assert.assertTrue(CustomizeClientRequestTestFilter.isInvoked());
        Assert.assertTrue(CustomizeClientResponseTestFilter.isInvoked());
        Assert.assertTrue(providerFilter.isInvoked());
        Assert.assertTrue(clientFilter.isInvoked());
    }

    @Test
    public void testNormalHttpRequest() throws IOException {
        Assert.assertFalse(providerFilter.isInvoked());
        Assert.assertFalse(CustomizeContainerRequestTestFilter.isInvoked());
        Assert.assertFalse(CustomizeContainerResponseTestFilter.isInvoked());

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8583/rest/get/abc");
        CloseableHttpResponse response = httpClient.execute(httpGet);

        Assert.assertTrue(providerFilter.isInvoked());
        Assert.assertTrue(CustomizeContainerRequestTestFilter.isInvoked());
        Assert.assertTrue(CustomizeContainerResponseTestFilter.isInvoked());

    }
}
