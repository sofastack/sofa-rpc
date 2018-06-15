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

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ConfigValueHelperTest {

    @Test
    public void assertFalse() {
        Assert.assertTrue(ConfigValueHelper.assertFalse(null));
        Assert.assertTrue(ConfigValueHelper.assertFalse(""));
        Assert.assertTrue(ConfigValueHelper.assertFalse("fALse"));
        Assert.assertTrue(ConfigValueHelper.assertFalse("null"));
        Assert.assertFalse(ConfigValueHelper.assertFalse("xasda"));
    }

    @Test
    public void checkNormal() {
        ConfigValueHelper.checkNormal("aaa", "123abc-_.");
        try {
            ConfigValueHelper.checkNormal("aaa", "123abc-_.!");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }

    @Test
    public void checkNormalWithColon() {
        ConfigValueHelper.checkNormalWithColon("aaa", "123abc-_.:");
        try {
            ConfigValueHelper.checkNormalWithColon("aaa", "123abc-_.:!");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }

    @Test
    public void checkNormalWithComma() {
        ConfigValueHelper.checkNormalWithComma("aaa", "123abc-_.,");
        try {
            ConfigValueHelper.checkNormalWithComma("aaa", "123abc-_.,!");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }

    @Test
    public void checkNormalWithCommaColon() {
        ConfigValueHelper.checkNormalWithCommaColon("aaa", "123abc-_.,:");
        try {
            ConfigValueHelper.checkNormalWithCommaColon("aaa", "123abc-_.,:!");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }

    @Test
    public void checkPositiveInteger() {
        ConfigValueHelper.checkPositiveInteger("aaa", 1);
        try {
            ConfigValueHelper.checkPositiveInteger("aaa", 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
        try {
            ConfigValueHelper.checkPositiveInteger("aaa", -1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }

    @Test
    public void checkNotNegativeInteger() {
        ConfigValueHelper.checkNotNegativeInteger("aaa", 1);
        ConfigValueHelper.checkNotNegativeInteger("aaa", 0);
        try {
            ConfigValueHelper.checkNotNegativeInteger("aaa", -1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("aaa"));
        }
    }
}