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
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class JSONUtilsTest {
    @Test
    public void toJSONString() throws Exception {
        Assert.assertEquals(JSONUtils.toJSONString("xxx"), "\"xxx\"");
        Assert.assertEquals(JSONUtils.toJSONString(1), "1");
        Assert.assertEquals(JSONUtils.toJSONString(2.2d), "2.2");
        Assert.assertEquals(JSONUtils.toJSONString(false), "false");
        Assert.assertEquals(JSONUtils.toJSONString(new HashMap()), "{}");
        Assert.assertEquals(JSONUtils.toJSONString(new ArrayList()), "[]");
        Assert.assertEquals(JSONUtils.toJSONString(new Object[0]), "[]");
    }

    @Test
    public void parseObject() throws Exception {
        Assert.assertTrue(JSONUtils.parseObject("\"true\"", boolean.class));
        Assert.assertEquals("s", JSONUtils.parseObject("\"s\"", String.class));
        Assert.assertEquals((Integer) 1, JSONUtils.parseObject("\"1\"", Integer.class));
        Assert.assertEquals(new HashMap(), JSONUtils.parseObject("{}", Map.class));
        Assert.assertEquals(new ArrayList(), JSONUtils.parseObject("[]", List.class));
        Assert.assertEquals(new Object[0], JSONUtils.parseObject("[]", Object[].class));
    }

}
