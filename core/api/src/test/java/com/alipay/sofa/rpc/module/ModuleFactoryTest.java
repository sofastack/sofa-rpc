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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ModuleFactoryTest {
    @Test
    public void needLoad() throws Exception {
        Assert.assertTrue(ModuleFactory.needLoad("*", "xxx"));
        Assert.assertTrue(ModuleFactory.needLoad("*,xxx", "xxx"));
        Assert.assertTrue(ModuleFactory.needLoad("xxx", "xxx"));
        Assert.assertTrue(ModuleFactory.needLoad("xxx,yyy", "xxx"));
        Assert.assertTrue(ModuleFactory.needLoad("yyy,xxx", "xxx"));
        Assert.assertTrue(ModuleFactory.needLoad("yyy,xxx,zzz", "xxx"));

        Assert.assertFalse(ModuleFactory.needLoad("", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("yyy", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("xxxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("xxxx,yyy", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("yyy,xxxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("*,-xxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("a,b,-xxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("xxx,-xxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("*,!xxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("a,b,!xxx", "xxx"));
        Assert.assertFalse(ModuleFactory.needLoad("xxx,!xxx", "xxx"));
    }

    @Test
    public void testAll() throws Exception {
        String old = RpcConfigs.getStringValue(RpcOptions.MODULE_LOAD_LIST);
        try {
            RpcConfigs.putValue(RpcOptions.MODULE_LOAD_LIST, "*,-test3");

            ModuleFactory.installModules();
            Assert.assertFalse(ModuleFactory.INSTALLED_MODULES.isEmpty());
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.containsKey("test"));
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.containsKey("test2"));
            Assert.assertFalse(ModuleFactory.INSTALLED_MODULES.containsKey("testNot"));
            Assert.assertEquals("testi", TestModules.test);
            Assert.assertEquals("test2i", TestModules.test2);
            Assert.assertNull(TestModules.testNot);

            TestModules.error = true;
            ModuleFactory.uninstallModule("test");
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.containsKey("test"));
            TestModules.error = false;

            TestModules.error = false;
            ModuleFactory.uninstallModule("test");
            Assert.assertFalse(ModuleFactory.INSTALLED_MODULES.containsKey("test"));
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.containsKey("test2"));
            Assert.assertEquals("testu", TestModules.test);
            Assert.assertEquals("test2i", TestModules.test2);
            Assert.assertNull(TestModules.testNot);

            TestModules.error = true;
            ModuleFactory.uninstallModules();
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.containsKey("test2"));
            TestModules.error = false;

            ModuleFactory.uninstallModules();
            Assert.assertTrue(ModuleFactory.INSTALLED_MODULES.isEmpty());
            Assert.assertEquals("testu", TestModules.test);
            Assert.assertEquals("test2u", TestModules.test2);
            Assert.assertNull(TestModules.testNot);
        } finally {
            if (old != null) {
                RpcConfigs.putValue(RpcOptions.MODULE_LOAD_LIST, old);
            }
        }
    }
}