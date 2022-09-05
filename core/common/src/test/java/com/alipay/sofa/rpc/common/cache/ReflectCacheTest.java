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
package com.alipay.sofa.rpc.common.cache;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class ReflectCacheTest {

    @After
    public void teardown() {
        ReflectCache.clearAll();
    }

    @Test
    public void putOverloadMethodCache() throws NoSuchMethodException {
        Method hashCode1 = String.class.getMethod("hashCode");
        Method hashCode2 = Object.class.getMethod("hashCode");
        ReflectCache.putOverloadMethodCache("service", hashCode1);
        Assert.assertSame(hashCode1, ReflectCache.getOverloadMethodCache("service", "hashCode", new String[] {}));

        ReflectCache.putOverloadMethodCache("service", hashCode2);
        Assert.assertSame(hashCode2, ReflectCache.getOverloadMethodCache("service", "hashCode", new String[] {}));
    }

    @Test
    public void putMethodCache() throws NoSuchMethodException {
        Method hashCode1 = String.class.getMethod("hashCode");
        Method hashCode2 = Object.class.getMethod("hashCode");
        ReflectCache.putMethodCache("service", hashCode1);
        Assert.assertSame(hashCode1, ReflectCache.getMethodCache("service", "hashCode"));

        ReflectCache.putMethodCache("service", hashCode2);
        Assert.assertSame(hashCode2, ReflectCache.getMethodCache("service", "hashCode"));
    }

    @Test
    public void putMethodSigsCache() throws NoSuchMethodException {
        String[] sign1 = { "a" };
        String[] sign2 = { "b" };
        ReflectCache.putMethodSigsCache("service", "hashCode", sign1);
        Assert.assertSame(sign1, ReflectCache.getMethodSigsCache("service", "hashCode"));

        ReflectCache.putMethodSigsCache("service", "hashCode", sign2);
        Assert.assertSame(sign2, ReflectCache.getMethodSigsCache("service", "hashCode"));
    }
}