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
package com.alipay.sofa.rpc.registry.sofa;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.client.ProviderStatus;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanggeng on 2017/7/13.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class SofaRegistryHelperTest {

    @Test
    public void getValue() throws Exception {

        SofaRegistryHelper.getValue(null);
        SofaRegistryHelper.getValue(null, null);

        Map<String, String> map = new HashMap<String, String>();
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "xx"), null);

        map.put("11", "aa");
        map.put("22", "bb");
        map.put("33", "cc");

        Assert.assertEquals(SofaRegistryHelper.getValue(map), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "xx"), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "yy", "zz"), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "zz", "11"), "aa");
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "11", "22"), "aa");
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "22", "33"), "bb");
    }

    @Test
    public void removeKeys() throws Exception {

        SofaRegistryHelper.removeOldKeys(null);
        SofaRegistryHelper.removeOldKeys(null, null);

        Map<String, String> map = new HashMap<String, String>();
        SofaRegistryHelper.removeOldKeys(map, null);

        map.put("11", "aa");
        map.put("22", "bb");
        map.put("33", "cc");

        SofaRegistryHelper.removeOldKeys(map);
        Assert.assertEquals(map.size(), 3);
        SofaRegistryHelper.removeOldKeys(map, "xx");
        Assert.assertEquals(map.size(), 3);
        SofaRegistryHelper.removeOldKeys(map, "xx", "yy");
        Assert.assertEquals(map.size(), 3);
        SofaRegistryHelper.removeOldKeys(map, "11");
        Assert.assertEquals(map.size(), 2);
        SofaRegistryHelper.removeOldKeys(map, "22", "33");
        Assert.assertEquals(map.size(), 0);

        map.put("11", "aa");
        map.put("22", "bb");
        map.put("33", "cc");

        Assert.assertEquals(SofaRegistryHelper.getValue(map), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "xx"), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "yy", "zz"), null);
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "zz", "11"), "aa");
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "11", "22"), "aa");
        Assert.assertEquals(SofaRegistryHelper.getValue(map, "22", "33"), "bb");
    }

    @Test
    public void convertProviderToUrls() throws Exception {
        ServerConfig serverConfig = new ServerConfig()
            .setHost("0.0.0.0")
            .setPort(22000)
            .setProtocol("bolt");

        ServerConfig serverConfig2 = new ServerConfig()
            .setHost("127.0.0.1")
            .setPort(12200)
            .setProtocol("tr");

        ServerConfig serverConfig3 = new ServerConfig()
            .setHost("192.1.1.1")
            .setPort(8080)
            .setProtocol("xfire");
        ProviderConfig<?> providerConfig = new ProviderConfig();
        providerConfig
            .setInterfaceId("com.alipay.sofa.rpc.test.TestService")
            .setUniqueId("qqqq")
            .setApplication(new ApplicationConfig().setAppName("xxxx"))
            .setTimeout(4444)
            .setWeight(250)
            .setServer(Arrays.asList(serverConfig, serverConfig2));

        MethodConfig methodConfig = new MethodConfig().setName("echo").setTimeout(3333);
        MethodConfig methodConfig2 = new MethodConfig().setName("xx").setTimeout(2222);
        providerConfig.setMethods(Arrays.asList(methodConfig, methodConfig2));

        String s1 = SofaRegistryHelper.convertProviderToUrls(providerConfig, serverConfig);
        Assert.assertNotNull(s1);
        ProviderInfo providerInfo = SofaRegistryHelper.parseProviderInfo(s1);
        Assert.assertEquals(SystemInfo.getLocalHost(), providerInfo.getHost());
        Assert.assertEquals(serverConfig.getPort(), providerInfo.getPort());
        Assert.assertEquals(providerConfig.getAppName(), providerInfo.getAttr(ProviderInfoAttrs.ATTR_APP_NAME));
        Assert.assertEquals(providerConfig.getTimeout(), providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT));

        String s2 = SofaRegistryHelper.convertProviderToUrls(providerConfig, serverConfig2);
        Assert.assertNotNull(s2);
        ProviderInfo providerInfo2 = SofaRegistryHelper.parseProviderInfo(s2);
        Assert.assertEquals(SystemInfo.getLocalHost(), providerInfo.getHost());
        Assert.assertEquals(serverConfig2.getPort(), providerInfo2.getPort());
        Assert.assertEquals(providerConfig.getAppName(), providerInfo2.getAttr(ProviderInfoAttrs.ATTR_APP_NAME));
        Assert.assertEquals(providerConfig.getTimeout(), providerInfo2.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT));

        String s3 = SofaRegistryHelper.convertProviderToUrls(providerConfig, serverConfig3);
        Assert.assertNotNull(s3);
        ProviderInfo providerInfo3 = SofaRegistryHelper.parseProviderInfo(s3);
        Assert.assertEquals(serverConfig3.getHost(), providerInfo3.getHost());
        Assert.assertEquals(serverConfig3.getPort(), providerInfo3.getPort());
        Assert.assertEquals(providerConfig.getAppName(), providerInfo3.getAttr(ProviderInfoAttrs.ATTR_APP_NAME));
        Assert.assertEquals(providerConfig.getTimeout(), providerInfo3.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT));
    }

    @Test
    public void parseProviderInfo() throws Exception {
        String defaultProtocol = RpcConfigs.getStringValue(RpcOptions.DEFAULT_PROTOCOL);
        // 10.244.22.1:8080?zone=GZ00A&self_app_name=icardcenter&app_name=icardcenter&_TIMEOUT=3000
        // 11.166.0.239:12200?_TIMEOUT=3000&p=1&_SERIALIZETYPE=hessian2&app_name=iptcore&zone=GZ00A&_IDLETIMEOUT=27&_MAXREADIDLETIME=30&v=4.0
        // 10.209.76.82:12200?_TIMEOUT=3000&p=1&_SERIALIZETYPE=hessian2&app_name=ipayprocess&zone=GZ00A&_IDLETIMEOUT=27&_MAXREADIDLETIME=30&v=4.0
        // 10.15.232.229:55555?_CONNECTIONNUM=1&v=4.0&_SERIALIZETYPE=4&app_name=test&p=1&_TIMEOUT=4000
        // 10.15.232.229:12222?_TIMEOUT=3333&p=1&_SERIALIZETYPE=4&_CONNECTIONNUM=1&_WARMUPTIME=60000&_WARMUPWEIGHT=5&app_name=test-server&v=4.0&_WEIGHT=2000&[cd]=[clientTimeout#5555]&[echoStr]=[timeout#4444]
        // [xxxx]=[clientTimeout#2000@retries#2]
        // [xxxx]=[_AUTORECONNECT#false@_TIMEOUT#2000]
        String url = "10.244.22.1:8080?zone=GZ00A&self_app_name=icardcenter&app_name=icardcenter&_TIMEOUT=3000";
        ProviderInfo provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(defaultProtocol.equals(provider.getProtocolType()));
        Assert.assertTrue("10.244.22.1".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 8080);
        Assert.assertTrue("GZ00A".equals(provider.getAttr("zone")));
        Assert.assertTrue("icardcenter".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3000);

        url = "11.166.0.239:12200?_TIMEOUT=3000&p=1&_SERIALIZETYPE=hessian2&app_name=iptcore&zone=GZ00B&_IDLETIMEOUT=27&_MAXREADIDLETIME=30&v=4.0";
        provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_BOLT.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_HESSIAN2.equals(provider.getSerializationType()));
        Assert.assertTrue("11.166.0.239".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 12200);
        Assert.assertTrue("GZ00B".equals(provider.getAttr("zone")));
        Assert.assertTrue("iptcore".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue("27".equals(provider.getAttr("_IDLETIMEOUT")));
        Assert.assertTrue("30".equals(provider.getAttr("_MAXREADIDLETIME")));
        Assert.assertTrue("4.0".equals(provider.getAttr("v")));
        Assert.assertTrue("1".equals(provider.getAttr("p")));

        url = "10.209.80.104:12200?zone=GZ00A&self_app_name=icif&_SERIALIZETYPE=java&app_name=icif&_TIMEOUT=3000";
        provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(defaultProtocol.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_JAVA.equals(provider.getSerializationType()));
        Assert.assertTrue("10.209.80.104".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 12200);
        Assert.assertTrue("GZ00A".equals(provider.getAttr("zone")));
        Assert.assertTrue("icif".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3000);
        Assert.assertTrue(provider.getAttr("v") == null);
        Assert.assertTrue(provider.getAttr("p") == null);

        url = "10.209.76.82:12200?_TIMEOUT=3000&p=13&_SERIALIZETYPE=11&app_name=ipayprocess&zone=GZ00A&_IDLETIMEOUT=27&_MAXREADIDLETIME=30&v=4.0&[xx]=[_AUTORECONNECT#false@_TIMEOUT#2000]";
        provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_TR.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_PROTOBUF.equals(provider.getSerializationType()));
        Assert.assertTrue("10.209.76.82".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 12200);
        Assert.assertTrue("GZ00A".equals(provider.getAttr("zone")));
        Assert.assertTrue("ipayprocess".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3000);
        Assert.assertTrue("27".equals(provider.getAttr("_IDLETIMEOUT")));
        Assert.assertTrue("30".equals(provider.getAttr("_MAXREADIDLETIME")));
        Assert.assertTrue("4.0".equals(provider.getAttr("v")));
        Assert.assertTrue("13".equals(provider.getAttr("p")));
        Assert.assertTrue((Integer) provider.getDynamicAttr(".xx.timeout") == 2000);
        Assert.assertTrue("false".equals(provider.getAttr(".xx._AUTORECONNECT")));

        url = "tri://10.15.232.229:55555?_CONNECTIONNUM=1&v=4.0&_SERIALIZETYPE=11&app_name=test&p=1&_TIMEOUT=4000";
        provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_TRIPLE.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_PROTOBUF.equals(provider.getSerializationType()));
        Assert.assertTrue("10.15.232.229".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 55555);
        Assert.assertTrue("test".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue("1".equals(provider.getAttr(ProviderInfoAttrs.ATTR_CONNECTIONS)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 4000);
        Assert.assertTrue("4.0".equals(provider.getAttr("v")));
        Assert.assertTrue("1".equals(provider.getAttr("p")));

        url = "10.15.232.229:12222?_TIMEOUT=3333&p=1&_SERIALIZETYPE=4&_CONNECTIONNUM=1&_WARMUPTIME=6&_WARMUPWEIGHT=5&app_name=test-server&v=4.0&_WEIGHT=2000&[cd]=[]&[echoStr]=[clientTimeout#4444]";
        provider = SofaRegistryHelper.parseProviderInfo(url);

        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_BOLT.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_HESSIAN2.equals(provider.getSerializationType()));
        Assert.assertTrue("10.15.232.229".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 12222);
        Assert.assertTrue("test-server".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue("1".equals(provider.getAttr(ProviderInfoAttrs.ATTR_CONNECTIONS)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3333);
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT) == 5);
        Assert.assertTrue(provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME) != null);
        Assert.assertEquals(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT), "5");
        Assert.assertEquals(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME), "6");
        Assert.assertTrue(provider.getWeight() == 5);
        Assert.assertTrue(provider.getStatus() == ProviderStatus.WARMING_UP);
        try {
            Thread.sleep(10);
        } catch (Exception e) {
        }
        Assert.assertTrue(provider.getWeight() == 2000);
        Assert.assertTrue(provider.getStatus() == ProviderStatus.AVAILABLE);
        Assert.assertTrue(provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME) == null);
        Assert.assertTrue("4.0".equals(provider.getAttr("v")));
        Assert.assertTrue("1".equals(provider.getAttr("p")));
        Assert.assertTrue(provider.getAttr(".cd.timeout") == null);
        Assert.assertTrue((Integer) provider.getDynamicAttr(".echoStr.timeout") == 4444);

        url = "10.15.232.229:12222?_TIMEOUT=3333&p=1&_SERIALIZETYPE=4&_CONNECTIONNUM=1&_WARMUPTIME=6&_WARMUPWEIGHT=5&startTime=123456";
        provider = SofaRegistryHelper.parseProviderInfo(url);
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3333);
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT) == 5);
        Assert.assertTrue(provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME) != null);
        Assert.assertEquals(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT), "5");
        Assert.assertEquals(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME), "6");
        Assert.assertTrue(provider.getWeight() == 100);
        Assert.assertTrue(provider.getStatus() == ProviderStatus.AVAILABLE);

        url = "bolt://10.244.22.1:8080?zone=GZ00A&appName=icardcenter&timeout=3000&serialization=hessian2";
        provider = SofaRegistryHelper.parseProviderInfo(url);
        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_BOLT.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_HESSIAN2.equals(provider.getSerializationType()));
        Assert.assertTrue("10.244.22.1".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 8080);
        Assert.assertTrue("GZ00A".equals(provider.getAttr("zone")));
        Assert.assertTrue("icardcenter".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue((Integer) provider.getDynamicAttr(ProviderInfoAttrs.ATTR_TIMEOUT) == 3000);
    }

    @Test
    public void parseMethodInfo() throws Exception {
        // 不用测试null等情况，
        Map<String, Object> map = new HashMap<String, Object>();
        SofaRegistryHelper.parseMethodInfo(map, "xx", "[]");
        Assert.assertTrue(map.size() == 0);

        map.clear();
        SofaRegistryHelper.parseMethodInfo(map, "xx", "[xxxx]");
        Assert.assertTrue(map.size() == 0);

        map.clear();
        SofaRegistryHelper.parseMethodInfo(map, "xx", "[clientTimeout#5555]");
        Assert.assertTrue(map.size() == 1);
        Assert.assertTrue(5555 == (Integer) map.get(".xx.timeout"));

        map.clear();
        SofaRegistryHelper.parseMethodInfo(map, "xx", "[_AUTORECONNECT#false@_TIMEOUT#2000]");
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(2000 == (Integer) map.get(".xx.timeout"));
        Assert.assertTrue("false".equals(map.get(".xx._AUTORECONNECT")));

        map.clear();
        SofaRegistryHelper.parseMethodInfo(map, "xx", "[clientTimeout#4444@retries#3]");
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(4444 == (Integer) map.get(".xx.timeout"));
        Assert.assertTrue("3".equals(map.get(".xx.retries")));
    }

}