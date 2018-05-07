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
package com.alipay.sofa.rpc.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderGroupTest {
    @Test
    public void getName() throws Exception {
        ProviderGroup pg = new ProviderGroup(null, null);
        Assert.assertNull(pg.getName());

        pg = new ProviderGroup("xxx");
        Assert.assertEquals(pg.getName(), "xxx");

        pg = new ProviderGroup("xxx", null);
        Assert.assertEquals(pg.getName(), "xxx");
    }

    @Test
    public void getProviderInfos() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        Assert.assertNotNull(pg.getProviderInfos());
        Assert.assertTrue(pg.getProviderInfos().size() == 0);
    }

    @Test
    public void setProviderInfos() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        List list = pg.getProviderInfos();
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 0);

        List<ProviderInfo> newps = new ArrayList<ProviderInfo>();
        pg.setProviderInfos(newps);
        Assert.assertNotNull(list);
        Assert.assertTrue(list != pg.getProviderInfos());
        Assert.assertTrue(newps == pg.getProviderInfos());

        newps = Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        pg.setProviderInfos(newps);
        Assert.assertNotNull(list);
        Assert.assertTrue(newps == pg.getProviderInfos());
        Assert.assertTrue(pg.getProviderInfos().size() == 1);
    }

    @Test
    public void isEmpty() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        Assert.assertTrue(pg.isEmpty());

        pg = new ProviderGroup("xxx", new ArrayList<ProviderInfo>());
        Assert.assertTrue(pg.isEmpty());

        pg = new ProviderGroup("xxx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200")));
        Assert.assertTrue(!pg.isEmpty());
    }

    @Test
    public void size() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        Assert.assertTrue(pg.size() == 0);

        pg = new ProviderGroup("xxx", new ArrayList<ProviderInfo>());
        Assert.assertTrue(pg.size() == 0);

        pg = new ProviderGroup("xxx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200")));
        Assert.assertTrue(pg.size() == 1);
    }

    @Test
    public void add() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        Assert.assertTrue(pg.size() == 0);

        pg.add(null);
        Assert.assertTrue(pg.size() == 0);

        pg.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        Assert.assertTrue(pg.size() == 1);

        pg.add(ProviderHelper.toProviderInfo("127.0.0.1:12201"));
        Assert.assertTrue(pg.size() == 2);

        // 重复
        pg.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        Assert.assertTrue(pg.size() == 2);
    }

    @Test
    public void addAll() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", null);
        Assert.assertTrue(pg.size() == 0);

        pg.addAll(null);
        Assert.assertTrue(pg.size() == 0);

        pg.addAll(new ArrayList<ProviderInfo>());
        Assert.assertTrue(pg.size() == 0);

        pg.addAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201")));
        Assert.assertTrue(pg.size() == 2);

        pg.addAll(Collections.singleton(ProviderHelper.toProviderInfo("127.0.0.1:12202")));
        Assert.assertTrue(pg.size() == 3);

        // 重复
        pg.addAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203")));
        Assert.assertTrue(pg.size() == 4);

        // 重复
        pg.addAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202")));
        Assert.assertTrue(pg.size() == 4);
    }

    @Test
    public void remove() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203")));
        Assert.assertTrue(pg.size() == 3);

        pg.remove(null);
        Assert.assertTrue(pg.size() == 3);

        pg.remove(ProviderHelper.toProviderInfo("127.0.0.1:12204"));
        Assert.assertTrue(pg.size() == 3);

        pg.remove(ProviderHelper.toProviderInfo("127.0.0.1:12203"));
        Assert.assertTrue(pg.size() == 2);

        // 重复
        pg.remove(ProviderHelper.toProviderInfo("127.0.0.1:12203"));
        Assert.assertTrue(pg.size() == 2);

        pg.remove(ProviderHelper.toProviderInfo("127.0.0.1:12202"));
        Assert.assertTrue(pg.size() == 1);
    }

    @Test
    public void removeAll() throws Exception {
        ProviderGroup pg = new ProviderGroup("xxx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203"),
            ProviderHelper.toProviderInfo("127.0.0.1:12204")));
        Assert.assertTrue(pg.size() == 4);

        pg.removeAll(null);
        Assert.assertTrue(pg.size() == 4);

        pg.removeAll(new ArrayList<ProviderInfo>());
        Assert.assertTrue(pg.size() == 4);
        // 删没有的
        pg.removeAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12205"),
            ProviderHelper.toProviderInfo("127.0.0.1:12206")));
        Assert.assertTrue(pg.size() == 4);
        // 删部分有的
        pg.removeAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12204"),
            ProviderHelper.toProviderInfo("127.0.0.1:12205")));
        Assert.assertTrue(pg.size() == 3);
        // 删都有的
        pg.removeAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12202"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203")));
        Assert.assertTrue(pg.size() == 1);
        // 重复删
        pg.removeAll(Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12202"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203")));
        Assert.assertTrue(pg.size() == 1);
    }
}