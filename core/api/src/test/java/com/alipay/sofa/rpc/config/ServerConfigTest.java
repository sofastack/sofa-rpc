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
package com.alipay.sofa.rpc.config;

import com.alipay.sofa.rpc.listener.ChannelListener;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerConfigTest {

    @Test
    public void testAll() {
        ServerConfig config = new ServerConfig();
        config.setProtocol("bolt")
            .setHost("127.0.0.2")
            .setPort(54321)
            .setContextPath("/")
            .setIoThreads(4)
            .setThreadPoolType("fixed")
            .setCoreThreads(20)
            .setMaxThreads(200)
            .setTelnet(false)
            .setQueueType("priority")
            .setQueues(1024)
            .setAliveTime(30000)
            .setPreStartCore(true)
            .setAccepts(30000)
            .setPayload(10 * 1024 * 1024)
            .setSerialization("protobuf")
            .setDispatcher("message")
            .setParameters(Collections.singletonMap("app", "app"))
            .setVirtualHost("192.168.1.1")
            .setVirtualPort(12345)
            .setOnConnect(Arrays.<ChannelListener> asList(new ChannelListener() {

                @Override
                public void onConnected(AbstractChannel channel) {
                }

                @Override
                public void onDisconnected(AbstractChannel channel) {
                }
            })).setEpoll(true)
            .setDaemon(false)
            .setAdaptivePort(true)
            .setTransport("netty")
            .setAutoStart(true)
            .setStopTimeout(10000)
            .setKeepAlive(true);

        Assert.assertEquals("bolt", config.getProtocol());
        Assert.assertEquals("127.0.0.2", config.getHost());
        Assert.assertEquals(54321, config.getPort());

        Assert.assertEquals("/", config.getContextPath());
        Assert.assertEquals(4, config.getIoThreads());
        Assert.assertEquals("fixed", config.getThreadPoolType());
        Assert.assertEquals(20, config.getCoreThreads());
        Assert.assertEquals(200, config.getMaxThreads());
        Assert.assertEquals(false, config.isTelnet());
        Assert.assertEquals("priority", config.getQueueType());
        Assert.assertEquals(1024, config.getQueues());
        Assert.assertEquals(30000, config.getAliveTime());

        Assert.assertEquals(true, config.isPreStartCore());
        Assert.assertEquals(30000, config.getAccepts());
        Assert.assertEquals(10 * 1024 * 1024, config.getPayload());
        Assert.assertEquals("protobuf", config.getSerialization());
        Assert.assertEquals("message", config.getDispatcher());
        Assert.assertEquals(1, config.getParameters().size());
        Assert.assertEquals("app", config.getParameters().get("app"));
        Assert.assertEquals("192.168.1.1", config.getVirtualHost());
        Assert.assertEquals(12345, (int) config.getVirtualPort());
        Assert.assertEquals(1, config.getOnConnect().size());

        Assert.assertEquals(true, config.isEpoll());
        Assert.assertEquals(false, config.isDaemon());
        Assert.assertEquals(true, config.isAdaptivePort());
        Assert.assertEquals("netty", config.getTransport());
        Assert.assertEquals(true, config.isAutoStart());
        Assert.assertEquals(10000, config.getStopTimeout());
        Assert.assertEquals(true, config.isKeepAlive());

        Assert.assertTrue(config.toString().contains("bolt"));
    }

    @Test
    public void testEquals() {
        ServerConfig config1 = new ServerConfig();
        Assert.assertTrue(config1.equals(config1));
        Assert.assertFalse(config1.equals(null));
        Assert.assertFalse(config1.equals(""));

        ServerConfig config2 = new ServerConfig();
        Assert.assertTrue(config1.equals(config2));

        config1.setHost("127.0.0.1");
        Assert.assertFalse(config1.equals(config2));
        config2.setHost("127.0.0.2");
        Assert.assertFalse(config1.equals(config2));
        config2.setHost("127.0.0.1");
        Assert.assertTrue(config1.equals(config2));

        config1.setPort(1234);
        Assert.assertFalse(config1.equals(config2));
        config2.setPort(1235);
        Assert.assertFalse(config1.equals(config2));
        config2.setPort(1234);
        Assert.assertTrue(config1.equals(config2));

        config1.setProtocol("xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setProtocol("yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setProtocol("xxx");
        Assert.assertTrue(config1.equals(config2));
    }

    @Test
    public void testHashCode() {
        ServerConfig config1 = new ServerConfig();
        ServerConfig config2 = new ServerConfig();
        config1.setHost("127.0.0.1").setPort(1234).setProtocol("xxx");
        config2.setHost("127.0.0.1").setPort(1235).setProtocol("xxx");
        Assert.assertFalse(config1.hashCode() == config2.hashCode());
        config2.setPort(1234);
        Assert.assertTrue(config1.hashCode() == config2.hashCode());
    }
}