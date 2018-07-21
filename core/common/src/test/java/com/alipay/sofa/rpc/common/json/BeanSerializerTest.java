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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BeanSerializerTest {

    @Test
    public void serialize() {
        TestJsonBean bean = new TestJsonBean();
        boolean error = false;
        try {
            Map map = (Map) BeanSerializer.serialize(bean);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        error = false;
        bean.setName("zzzggg");
        try {
            Map map = (Map) BeanSerializer.serialize(bean, true);
            Assert.assertEquals(map.get("Name"), "zzzggg");
            Assert.assertEquals(map.get("Sex"), false);
            Assert.assertEquals(map.get("age"), 0);
            Assert.assertFalse(map.containsKey("friends"));
            Assert.assertTrue(map.containsKey("Remark"));
            Assert.assertTrue(map.containsKey(JSON.CLASS_KEY));

            map = (Map) BeanSerializer.serialize(bean);
            Assert.assertEquals(map.get("Name"), "zzzggg");
            Assert.assertEquals(map.get("Sex"), false);
            Assert.assertEquals(map.get("age"), 0);
            Assert.assertFalse(map.containsKey("friends"));
            Assert.assertTrue(map.containsKey("Remark"));
            Assert.assertFalse(map.containsKey(JSON.CLASS_KEY));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        bean.setName("zzzgg");
        bean.setSex(true);
        bean.setAge(111);
        bean.setStep(1234567890l);
        bean.setFriends(new ArrayList<TestJsonBean>());
        bean.setStatus(TestJsonBean.Status.START);
        error = false;
        try {
            Map map = (Map) BeanSerializer.serialize(bean, true);
            //System.out.println(map);
            Assert.assertEquals(map.get("Name"), "zzzgg");
            Assert.assertEquals(map.get("Sex"), true);
            Assert.assertEquals(map.get("age"), 111);
            Assert.assertTrue(map.containsKey("friends"));
            Assert.assertTrue(map.containsKey("Remark"));
            Assert.assertTrue(map.containsKey(JSON.CLASS_KEY));

            map = (Map) BeanSerializer.serialize(bean);
            //System.out.println(map);
            Assert.assertEquals(map.get("Name"), "zzzgg");
            Assert.assertEquals(map.get("Sex"), true);
            Assert.assertEquals(map.get("age"), 111);
            Assert.assertTrue(map.containsKey("friends"));
            Assert.assertTrue(map.containsKey("Remark"));
            Assert.assertFalse(map.containsKey(JSON.CLASS_KEY));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
    }

    @Test
    public void deserialize() throws Exception {

        List list3 = new ArrayList();
        list3.add(1111);
        list3.add("ll222");

        Map map4 = new HashMap();
        map4.put("key1", "111");
        map4.put("key2", 222);

        Map beanmap5 = new HashMap();
        beanmap5.put("Name", "zzzzggg");
        beanmap5.put("Sex", true);
        beanmap5.put("age", 22);
        beanmap5.put("friends", new ArrayList<TestJsonBean>());
        beanmap5.put("remark", "hehehe");

        TestJsonBean bean = (TestJsonBean) BeanSerializer.deserializeByType(beanmap5, TestJsonBean.class);
        Assert.assertEquals(beanmap5.get("Name"), bean.getName());
        Assert.assertEquals(beanmap5.get("Sex"), bean.isSex());
        Assert.assertEquals(beanmap5.get("age"), bean.getAge());
        Assert.assertEquals(beanmap5.get("friends"), bean.getFriends());
        Assert.assertEquals(beanmap5.get("Remark"), bean.getRemark());

        beanmap5.put(JSON.CLASS_KEY, "com.alipay.sofa.rpc.common.json.TestJsonBean");
        TestJsonBean bean2 = (TestJsonBean) BeanSerializer.deserializeByType(beanmap5, Object.class);
        Assert.assertEquals(beanmap5.get("Name"), bean2.getName());
        Assert.assertEquals(beanmap5.get("Sex"), bean2.isSex());
        Assert.assertEquals(beanmap5.get("age"), bean2.getAge());
        Assert.assertEquals(beanmap5.get("friends"), bean2.getFriends());
        Assert.assertEquals(beanmap5.get("Remark"), bean2.getRemark());

        map4.put("bean", beanmap5);
        list3.add(map4);
        list3.add(beanmap5);

        int[] ss6 = new int[] { 11, 22, 33, 44 };
        Integer[] ss7 = new Integer[] { 55, 66 };

        Object[] args = new Object[] { "11", 22, true, list3, map4, beanmap5, ss6, ss7 };
        Object[] os = BeanSerializer.deserializeByType(args, Object[].class);
        Assert.assertEquals(os[0], "11");
        Assert.assertEquals(os[1], 22);
        Assert.assertEquals(os[2], true);
        Assert.assertEquals(((List) os[3]).get(0), 1111);
        Assert.assertEquals(((List) os[3]).get(1), "ll222");
        Assert.assertTrue(((List) os[3]).get(2) instanceof Map);
        Assert.assertTrue(((List) os[3]).get(3) instanceof TestJsonBean);
        TestJsonBean actualBean3_3 = (TestJsonBean) ((List) os[3]).get(3);
        Assert.assertEquals(beanmap5.get("Name"), actualBean3_3.getName());
        Assert.assertEquals(beanmap5.get("Sex"), actualBean3_3.isSex());
        Assert.assertEquals(beanmap5.get("age"), actualBean3_3.getAge());
        Assert.assertEquals(beanmap5.get("friends"), actualBean3_3.getFriends());
        Assert.assertEquals(beanmap5.get("Remark"), actualBean3_3.getRemark());

        Assert.assertTrue(os[4] instanceof Map);
        Map actualMap4 = (Map) os[4];
        Assert.assertTrue(actualMap4.get("bean") instanceof TestJsonBean);

        Assert.assertTrue(os[5] instanceof TestJsonBean);
        TestJsonBean actualBean5 = (TestJsonBean) os[5];
        Assert.assertEquals(beanmap5.get("Name"), actualBean5.getName());
        Assert.assertEquals(beanmap5.get("Sex"), actualBean5.isSex());
        Assert.assertEquals(beanmap5.get("age"), actualBean5.getAge());
        Assert.assertEquals(beanmap5.get("friends"), actualBean5.getFriends());
        Assert.assertEquals(beanmap5.get("Remark"), actualBean5.getRemark());

        Assert.assertArrayEquals((int[]) os[6], ss6);
        Assert.assertArrayEquals((Integer[]) os[7], ss7);

        List<Map> list8 = new ArrayList<Map>();
        for (int i = 0; i < 2; i++) {
            Map beanmap1 = new HashMap();
            beanmap1.put("Name", "zzzzggg");
            beanmap1.put("Sex", true);
            beanmap1.put("age", 22);
            beanmap1.put("friends", new ArrayList<TestJsonBean>());
            beanmap1.put("remark", "hehehe");
            list8.add(beanmap1);
        }
        List jsonBean1 = (List) BeanSerializer.deserialize(list8);
        List jsonBean2 = BeanSerializer.deserializeByType(list8, List.class);
        Assert.assertEquals(jsonBean1.size(), jsonBean2.size());
        Assert.assertEquals(jsonBean1.get(0).getClass(), jsonBean2.get(0).getClass());

    }

    @Test
    public void serializeMap() {
        boolean error = false;
        Map<Object, Object> bean = createTestMap();
        try {
            Map map = (Map) BeanSerializer.serialize(bean, true);
            for (Map.Entry<Object, Object> entry : bean.entrySet()) {
                if (!map.containsKey(entry.getKey())) {
                    error = true;
                    break;
                }
            }
            Assert.assertFalse(error);

            map = (Map) BeanSerializer.serialize(bean);
            for (Map.Entry<Object, Object> entry : bean.entrySet()) {
                if (!map.containsKey(entry.getKey())) {
                    error = true;
                    break;
                }
            }
            Assert.assertFalse(error);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
    }

    private Map<Object, Object> createTestMap() {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(1, "1");
        map.put("2", 2);
        map.put("true", true);
        return map;
    }
}