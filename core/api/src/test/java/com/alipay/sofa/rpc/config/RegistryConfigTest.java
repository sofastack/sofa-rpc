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

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RegistryConfigTest {

    @Test
    public void testAll() {
        RegistryConfig config = new RegistryConfig();
        config.setProtocol("xx")
            .setAddress("127.0.0.1")
            .setFile("file")
            .setRegister(true)
            .setSubscribe(true)
            .setTimeout(1234)
            .setBatch(false)
            .setBatchSize(12)
            .setConnectTimeout(1235)
            .setHeartbeatPeriod(22222)
            .setIndex("null")
            .setParameters(Collections.singletonMap("app", "app"))
            .setParameter("token", "xxx")
            .setReconnectPeriod(3333);

        Assert.assertEquals("xx", config.getProtocol());
        Assert.assertEquals("127.0.0.1", config.getAddress());
        Assert.assertEquals("file", config.getFile());

        Assert.assertEquals(true, config.isRegister());
        Assert.assertEquals(true, config.isSubscribe());
        Assert.assertEquals(1234, config.getTimeout());
        Assert.assertEquals(false, config.isBatch());
        Assert.assertEquals(12, config.getBatchSize());
        Assert.assertEquals(1235, config.getConnectTimeout());
        Assert.assertEquals(22222, config.getHeartbeatPeriod());
        Assert.assertEquals("null", config.getIndex());
        Assert.assertEquals(2, config.getParameters().size());
        Assert.assertEquals("xxx", config.getParameter("token"));
        Assert.assertEquals(3333, config.getReconnectPeriod());

        RegistryConfig config1 = new RegistryConfig().setParameters(Collections.singletonMap("app", "app"));
        RegistryConfig config2 = new RegistryConfig().setParameter("app", "app");
        Assert.assertTrue(config1.equals(config2));

        Assert.assertTrue(config.toString().contains("RegistryConfig"));
    }

    @Test
    public void testEquals() {
        RegistryConfig config1 = new RegistryConfig();
        Assert.assertTrue(config1.equals(config1));
        Assert.assertFalse(config1.equals(null));
        Assert.assertFalse(config1.equals(""));

        RegistryConfig config2 = new RegistryConfig();
        Assert.assertTrue(config1.equals(config2));

        config1.setRegister(!config1.isRegister());
        Assert.assertFalse(config1.equals(config2));
        config2.setRegister(!config2.isRegister());
        Assert.assertTrue(config1.equals(config2));

        config1.setSubscribe(!config1.isSubscribe());
        Assert.assertFalse(config1.equals(config2));
        config2.setSubscribe(!config2.isSubscribe());
        Assert.assertTrue(config1.equals(config2));

        config1.setTimeout(9998);
        Assert.assertFalse(config1.equals(config2));
        config2.setTimeout(9997);
        Assert.assertFalse(config1.equals(config2));
        config2.setTimeout(9998);
        Assert.assertTrue(config1.equals(config2));

        config1.setConnectTimeout(9998);
        Assert.assertFalse(config1.equals(config2));
        config2.setConnectTimeout(9997);
        Assert.assertFalse(config1.equals(config2));
        config2.setConnectTimeout(9998);
        Assert.assertTrue(config1.equals(config2));

        config1.setBatch(!config1.isBatch());
        Assert.assertFalse(config1.equals(config2));
        config2.setBatch(!config2.isBatch());
        Assert.assertTrue(config1.equals(config2));

        config1.setBatchSize(9998);
        Assert.assertFalse(config1.equals(config2));
        config2.setBatchSize(9997);
        Assert.assertFalse(config1.equals(config2));
        config2.setBatchSize(9998);
        Assert.assertTrue(config1.equals(config2));

        config1.setHeartbeatPeriod(9998);
        Assert.assertFalse(config1.equals(config2));
        config2.setHeartbeatPeriod(9997);
        Assert.assertFalse(config1.equals(config2));
        config2.setHeartbeatPeriod(9998);
        Assert.assertTrue(config1.equals(config2));

        config1.setReconnectPeriod(9998);
        Assert.assertFalse(config1.equals(config2));
        config2.setReconnectPeriod(9997);
        Assert.assertFalse(config1.equals(config2));
        config2.setReconnectPeriod(9998);
        Assert.assertTrue(config1.equals(config2));

        config1.setProtocol("xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setProtocol("yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setProtocol("xxx");
        Assert.assertTrue(config1.equals(config2));

        config1.setAddress("xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setAddress("yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setAddress("xxx");
        Assert.assertTrue(config1.equals(config2));

        config1.setIndex("xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setIndex("yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setIndex("xxx");
        Assert.assertTrue(config1.equals(config2));

        config1.setFile("xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setFile("yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setFile("xxx");
        Assert.assertTrue(config1.equals(config2));

        config1.setParameter("xxx", "xxx");
        Assert.assertFalse(config1.equals(config2));
        config2.setParameter("xxx", "yyy");
        Assert.assertFalse(config1.equals(config2));
        config2.setParameter("xxx", "xxx");
        Assert.assertTrue(config1.equals(config2));
    }

    @Test
    public void testHashCode() {
        RegistryConfig config1 = new RegistryConfig();
        RegistryConfig config2 = new RegistryConfig();
        config1.setAddress("127.0.0.1:1234").setProtocol("xxx");
        config2.setAddress("127.0.0.1:1234").setProtocol("yyy");
        Assert.assertFalse(config1.hashCode() == config2.hashCode());
        config2.setProtocol("xxx");
        Assert.assertTrue(config1.hashCode() == config2.hashCode());
    }
}