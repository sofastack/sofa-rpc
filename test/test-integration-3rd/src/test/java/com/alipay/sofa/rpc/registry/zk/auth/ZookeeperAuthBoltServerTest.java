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
package com.alipay.sofa.rpc.registry.zk.auth;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.base.BaseZkTest;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jjzxjgy@126.com">jianyang</a>
 */
public class ZookeeperAuthBoltServerTest {

    protected final static Logger      LOGGER     = LoggerFactory.getLogger(ZookeeperAuthBoltServerTest.class);

    private static ServerConfig        serverConfig;
    private static RegistryConfig      registryConfig;

    private static Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Zookeeper zkClient
     */
    private static CuratorFramework    zkClient;

    @Before
    public void setUp() {
        BaseZkTest.adBeforeClass();
        createPathWithAuth();
    }

    @Test
    public void testUseCorrentAuth() {

        parameters.put("scheme", "digest");
        //如果存在多个认证信息，则在参数形式为为user1:passwd1,user2:passwd2
        parameters.put("addAuth", "sofazk:rpc1");

        registryConfig = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setAddress("127.0.0.1:2181/authtest")
            .setParameters(parameters);

        serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        ProviderConfig<EchoService> providerConfig = new ProviderConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setRef(new EchoServiceImpl()) // 指定实现
            .setServer(serverConfig); // 指定服务端
        providerConfig.export(); // 发布服务

        ConsumerConfig<EchoService> consumerConfig = new ConsumerConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setProtocol("bolt") // 指定协议
            .setTimeout(3000)
            .setConnectTimeout(10 * 1000);
        EchoService echoService = consumerConfig.refer();

        String result = echoService.echoStr("auth test");

        Assert.assertEquals("auth test", result);

    }

    @Test
    public void testUseNoMatchAuth() {

        parameters.put("scheme", "digest");
        //如果存在多个认证信息，则在参数形式为为user1:passwd1,user2:passwd2
        parameters.put("addAuth", "sofazk:rpc2");

        registryConfig = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setAddress("127.0.0.1:2181/authtest")
            .setParameters(parameters);

        serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        ProviderConfig<EchoService> providerConfig = new ProviderConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setRef(new EchoServiceImpl()) // 指定实现
            .setServer(serverConfig); // 指定服务端

        try {
            providerConfig.export(); // 发布服务
            Assert.fail("auth is not right, but publish success");
        } catch (Exception ex) {

            LOGGER.error("exception is", ex);

            if (ex.getCause() instanceof KeeperException.NoAuthException) {
                Assert.assertTrue(true);
            } else {
                Assert.fail("auth is not right, but throw not auth error exception");
            }
        }

        ConsumerConfig<EchoService> consumerConfig = new ConsumerConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setProtocol("bolt") // 指定协议
            .setTimeout(3000)
            .setConnectTimeout(10 * 1000);

        try {
            consumerConfig.refer();// 引用服务
            Assert.fail("auth is not right, but consumer refer success");
        } catch (Exception ex) {

            LOGGER.error("exception is", ex);

            if (ex.getCause() instanceof KeeperException.NoAuthException) {
                Assert.assertTrue(true);
            } else {
                Assert.fail("auth is not right, but throw not auth error exception");
            }
        }
    }

    /**
     * 创建认证信息
     *
     * @return
     */
    private List<AuthInfo> buildAuthInfo(Map<String, String> authMap) {
        List<AuthInfo> info = new ArrayList<AuthInfo>();

        String scheme = authMap.get("scheme");

        //如果存在多个认证信息，则在参数形式为为addAuth=user1:paasswd1,user2:passwd2
        String addAuth = authMap.get("addAuth");

        if (StringUtils.isNotEmpty(addAuth)) {
            String[] addAuths = addAuth.split(",");
            for (String singleAuthInfo : addAuths) {
                info.add(new AuthInfo(scheme, singleAuthInfo.getBytes()));
            }
        }

        return info;
    }

    /**
     * 获取默认的AclProvider
     *
     * @return
     */
    private static ACLProvider getDefaultAclProvider() {
        return new ACLProvider() {
            @Override
            public List<ACL> getDefaultAcl() {
                return ZooDefs.Ids.CREATOR_ALL_ACL;
            }

            @Override
            public List<ACL> getAclForPath(String path) {
                return ZooDefs.Ids.CREATOR_ALL_ACL;
            }
        };
    }

    protected void createPathWithAuth() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFrameworkFactory.Builder zkClientuilder = CuratorFrameworkFactory.builder()
            .connectString("127.0.0.1:2181")
            .sessionTimeoutMs(20000 * 3)
            .connectionTimeoutMs(20000)
            .canBeReadOnly(false)
            .retryPolicy(retryPolicy)
            .defaultData(null);

        //是否需要添加zk的认证信息
        Map authMap = new HashMap<String, String>();
        authMap.put("scheme", "digest");
        //如果存在多个认证信息，则在参数形式为为user1:passwd1,user2:passwd2
        authMap.put("addAuth", "sofazk:rpc1");

        List<AuthInfo> authInfos = buildAuthInfo(authMap);
        if (CommonUtils.isNotEmpty(authInfos)) {
            zkClientuilder = zkClientuilder.aclProvider(getDefaultAclProvider())
                .authorization(authInfos);
        }

        try {
            zkClient = zkClientuilder.build();
            zkClient.start();
            zkClient.create().withMode(CreateMode.PERSISTENT).forPath("/authtest");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @After
    public void destroy() {
        serverConfig.destroy();
        BaseZkTest.adAfterClass();
    }

}
