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
package com.alipay.sofa.rpc.common.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class JSONTest {

    @Test
    public void test() {
        TestJsonBean bean = new TestJsonBean();
        bean.setName("xxxx");
        bean.setAge(123);
        String str = JSON.toJSONString(bean);
        Assert.assertTrue(str.contains("\"Name\":\"xxxx\""));

        str = JSON.toJSONString(bean, true);
        Assert.assertTrue(str.contains(JSON.CLASS_KEY));
    }

    @Test
    public void testBeanWithMapSerialization() {
        TestJsonBean bean = new TestJsonBean();
        bean.setName("xxxx");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", new Object());
        bean.setMap(map);
        String jsonString = JSON.toJSONString(bean, true);
        bean.getMap().values().forEach(value -> Assert.assertEquals(value.getClass(), Object.class));
    }

    @Test
    public void testBeanWithInnerClassDeserialization() {
        TestJsonBean bean = new TestJsonBean();
        bean.setName("xxxx");
        String jsonString = JSON.toJSONString(bean, true);
        Assert.assertEquals(JSON.parseObject(jsonString, TestJsonBean.class).getInnerBean().getClass(),
            TestJsonBean.InnerBean.class);
    }

}