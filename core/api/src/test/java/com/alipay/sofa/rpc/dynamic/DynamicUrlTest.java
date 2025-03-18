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
package com.alipay.sofa.rpc.dynamic;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author Narziss
 * @version DynamicUrl.java, v 0.1 2024年10月28日 21:40 Narziss
 */
public class DynamicUrlTest {

    @Test
    public void testWithPathAndParams() {
        DynamicUrl dynamicUrl = new DynamicUrl("apollo://127.0.0.1:8080/config?appId=xxx&cluster=yyy");
        Assert.assertEquals("apollo", dynamicUrl.getProtocol());
        Assert.assertEquals("127.0.0.1", dynamicUrl.getHost());
        Assert.assertEquals(8080, dynamicUrl.getPort());
        Assert.assertEquals("/config", dynamicUrl.getPath());
        Assert.assertEquals("127.0.0.1:8080/config", dynamicUrl.getAddress());
        Assert.assertNotNull(dynamicUrl.getParams());
        Assert.assertEquals("xxx", dynamicUrl.getParams().get("appId"));
        Assert.assertEquals("yyy", dynamicUrl.getParams().get("cluster"));
    }

    @Test
    public void testWithSlashAndParams() {
        DynamicUrl dynamicUrl = new DynamicUrl("apollo://127.0.0.1:8080/?appId=xxx&cluster=yyy");
        Assert.assertEquals("apollo", dynamicUrl.getProtocol());
        Assert.assertEquals("127.0.0.1", dynamicUrl.getHost());
        Assert.assertEquals(8080, dynamicUrl.getPort());
        Assert.assertEquals("", dynamicUrl.getPath());// 如果路径为空，返回空字符串
        Assert.assertEquals("127.0.0.1:8080", dynamicUrl.getAddress());
        Assert.assertNotNull(dynamicUrl.getParams());
        Assert.assertEquals("xxx", dynamicUrl.getParams().get("appId"));
        Assert.assertEquals("yyy", dynamicUrl.getParams().get("cluster"));
    }

    @Test
    public void testWithParams() {
        DynamicUrl dynamicUrl = new DynamicUrl("apollo://127.0.0.1:8080?appId=xxx&cluster=yyy");
        Assert.assertEquals("apollo", dynamicUrl.getProtocol());
        Assert.assertEquals("127.0.0.1", dynamicUrl.getHost());
        Assert.assertEquals(8080, dynamicUrl.getPort());
        Assert.assertEquals("", dynamicUrl.getPath());
        Assert.assertEquals("127.0.0.1:8080", dynamicUrl.getAddress());
        Assert.assertNotNull(dynamicUrl.getParams());
        Assert.assertEquals("xxx", dynamicUrl.getParams().get("appId"));
        Assert.assertEquals("yyy", dynamicUrl.getParams().get("cluster"));
    }

    @Test
    public void testOnlyHostAndPort() {
        DynamicUrl dynamicUrl = new DynamicUrl("apollo://127.0.0.1:8080");
        Assert.assertEquals("apollo", dynamicUrl.getProtocol());
        Assert.assertEquals("127.0.0.1", dynamicUrl.getHost());
        Assert.assertEquals(8080, dynamicUrl.getPort());
        Assert.assertEquals("", dynamicUrl.getPath());
        Assert.assertEquals("127.0.0.1:8080", dynamicUrl.getAddress());
        System.out.println(dynamicUrl.getParams());
        Assert.assertNotNull(dynamicUrl.getParams());
        Assert.assertTrue(dynamicUrl.getParams().isEmpty());
    }
}
