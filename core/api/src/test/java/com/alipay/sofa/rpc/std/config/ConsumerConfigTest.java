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
package com.alipay.sofa.rpc.std.config;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import static org.junit.Assert.*;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.std.sample.SampleService;
import com.alipay.sofa.rpc.std.sample.SampleServiceImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author zhaowang
 * @version : ConsumerConfigTest.java, v 0.1 2022年01月28日 2:33 下午 zhaowang
 */
public class ConsumerConfigTest {

    private ConsumerConfig config;

    @Before
    public void before() {
        config = new ConsumerConfig();
    }

    @Test
    public void testDefaultValue() {
        assertEquals("bolt", config.getProtocol());
        assertEquals(null, config.getDirectUrl());
        assertEquals(false, config.isGeneric());
        assertEquals("sync", config.getInvokeType());
        assertEquals(1000, config.getConnectTimeout());
        assertEquals(10000, config.getDisconnectTimeout());
        assertEquals("failover", config.getCluster());
        assertEquals("all", config.getConnectionHolder());
        assertEquals("auto", config.getLoadBalancer());
        assertEquals(false, config.isLazy());
        assertEquals(false, config.isSticky());
        assertEquals(false, config.isInJVM());
        assertEquals(false, config.isCheck());
        assertEquals(1, config.getConnectionNum());
        assertEquals(30000, config.getHeartbeatPeriod());
        assertEquals(10000, config.getReconnectPeriod());
        assertEquals("DISCARD", config.getRejectedExecutionPolicy());
        assertEquals(null, config.getRouter());
        assertEquals(null, config.getRouterRef());
        assertEquals(null, config.getOnReturn());
        assertEquals(null, config.getOnConnect());
        assertEquals(null, config.getOnAvailable());
        assertEquals(null, config.getOnAvailable());
        assertEquals(-1, config.getAddressWait());
        assertEquals(3, config.getRepeatedReferLimit());
        assertEquals(-1, config.getTimeout());
        assertEquals(0, config.getRetries());
        assertEquals(0, config.getConcurrents());
    }

    @Test
    public void testGetProxyClass() {
        config = new ConsumerConfig();
        try {
            config.getProxyClass();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SofaRpcRuntimeException);
        }

        config = new ConsumerConfig();
        config.setInterfaceId(SampleService.class.getName());
        assertEquals(SampleService.class, config.getProxyClass());
        assertEquals(SampleService.class, config.getProxyClass());

        config = new ConsumerConfig();
        config.setInterfaceId(SampleServiceImpl.class.getName());
        try {
            config.getProxyClass();
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof SofaRpcRuntimeException);
        }

        // triple protocol allows proxy not an interface
        config = new ConsumerConfig();
        config.setInterfaceId(SampleServiceImpl.class.getName());
        config.setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE);
        config.getProxyClass();

        config = new ConsumerConfig();
        config.setGeneric(true);
        assertEquals(GenericService.class, config.getProxyClass());
    }

    @Test
    public void testGetInterfaceId() {
        String interfaceName = InnerInterface.class.getName();
        config.setInterfaceId(interfaceName);
        assertEquals(interfaceName, config.getInterfaceId());

        config.setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE);
        assertEquals("serviceName", config.getInterfaceId());
    }

    public interface InnerInterface {
    }

    public static String getServiceName() {
        return "serviceName";
    }

}