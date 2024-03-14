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

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FastjsonUtilsTest {

    private FastjsonUtils fastjsonUtilsTest;

    @Before
    public void setUp() throws Exception {
        fastjsonUtilsTest = new FastjsonUtils();
    }

    @Test
    public void testToJSONString() {
        assertEquals("obj", fastjsonUtilsTest.toJSONString("obj"));
    }

    @Test
    public void testToJSONStringWithObject() {
        assertEquals("{\"age\":1,\"name\":\"name\"}", fastjsonUtilsTest.toJSONString(new TestClass(1, "name")));
    }

    @Test
    public void testParseObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("age", 1);
        jsonObject.put("name", "name");
        assertEquals(jsonObject, FastjsonUtils.parseObject("{\"age\":1,\"name\":\"name\"}"));
    }

    @Test
    public void testParseObjectWithObject() {
        TestClass testClass = FastjsonUtils.parseObject("{\"age\":1,\"name\":\"name\"}", TestClass.class);
        assertEquals(1, testClass.age);
        assertEquals("name", testClass.name);
    }

    @Test
    public void testParseObjectWithMap() {
        Map<String, String> map = FastjsonUtils.parseObject("{\"age\":1,\"name\":\"name\"}",
            new TypeReference<Map<String, String>>() {
            });
        assertEquals("1", map.get("age"));
        assertEquals("name", map.get("name"));
    }

    public static class TestClass {
        int    age;

        String name;

        public TestClass(int age, String name) {
            this.age = age;
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
