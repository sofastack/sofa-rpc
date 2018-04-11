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
package com.alipay.sofa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BeanUtilsTest {

    @Test
    public void testSetProperty() throws Exception {
        TestBean config = new TestBean();
        BeanUtils.setProperty(config, "alias", String.class, "1111aaaa");
        Assert.assertEquals(config.getAlias(), "1111aaaa");
        BeanUtils.setProperty(config, "alias", String.class, null);
        Assert.assertNull(config.getAlias());

        BeanUtils.setProperty(config, "heartbeat", int.class, 3000);
        Assert.assertEquals(config.getHeartbeat(), 3000);

        BeanUtils.setProperty(config, "heartbeat", int.class, new Integer(7000));
        Assert.assertEquals(config.getHeartbeat(), 7000);

        boolean error = false;
        try {
            BeanUtils.setProperty(config, "xxx", String.class, "1111aaaa");
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testGetProperty() throws Exception {
        TestBean config = new TestBean();
        config.setAlias("1111aaaa");
        config.setHeartbeat(2000);
        config.setRegister(false);
        Assert.assertEquals(BeanUtils.getProperty(config, "alias", String.class), "1111aaaa");
        Assert.assertTrue(BeanUtils.getProperty(config, "heartbeat", int.class) == 2000);
        Assert.assertTrue((Integer) BeanUtils.getProperty(config, "heartbeat", null) == 2000);
        Assert.assertFalse(BeanUtils.getProperty(config, "register", boolean.class));
        boolean error = false;
        try {
            BeanUtils.getProperty(config, "xxx", String.class);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testCopyProptertiesToMap() throws Exception {
        TestBean config = new TestBean();
        config.setAlias("1111aaaa");
        Map<String, Object> map = new HashMap<String, Object>();
        BeanUtils.copyPropertiesToMap(config, "", map);
        Assert.assertTrue(map.size() > 0);
        Assert.assertEquals(map.get("alias"), "1111aaaa");
    }

    @Test
    public void testCopyPropterties() throws Exception {
        TestBean bean = new TestBean();
        bean.setAlias("aaa:1.0.0");
        List<TestSubBean> subBeans = new ArrayList<TestSubBean>();
        TestSubBean sub = new TestSubBean();
        sub.setName("xxxxxx");
        sub.setParameter("maaaaak", "maaaav");
        subBeans.add(sub);
        bean.setSubBeans(subBeans);

        TestOtherBean otherBean = new TestOtherBean();
        BeanUtils.copyProperties(bean, otherBean, "alias");

        Assert.assertEquals(bean.getHeartbeat(), otherBean.getHeartbeat());
        Assert.assertFalse(bean.getAlias().equals(otherBean.getAlias()));
        Assert.assertEquals(bean.getSubBeans(), otherBean.getSubBeans());
        Assert.assertEquals(bean.isRegister(), otherBean.isRegister());
    }

    @Test
    public void testGetModifiedFields() throws Exception {
        TestBean cg0 = new TestBean();

        TestBean cg1 = new TestBean();
        cg1.setAlias("aaa:1.0.0");
        cg1.setHeartbeat(2222);
        cg1.setRegister(true);

        TestBean cg2 = new TestBean();
        cg2.setAlias("aaa:1.0.0");
        cg2.setHeartbeat(2222);
        cg2.setRegister(false);
        cg2.setSubBeans(new ArrayList<TestSubBean>());

        Assert.assertTrue(BeanUtils.getModifiedFields(cg0, cg1).size() == 3);
        Assert.assertTrue(BeanUtils.getModifiedFields(cg0, cg2).size() == 3);
        Assert.assertTrue(BeanUtils.getModifiedFields(cg1, cg2).size() == 2);
    }
}