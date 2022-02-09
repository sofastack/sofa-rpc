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

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * @author zhaowang
 * @version : TestUtilsTest.java, v 0.1 2022年01月25日 5:46 下午 zhaowang
 */
public class TestUtilsTest {

    @Test
    public void testRandomString() {
        Pattern compile = Pattern.compile("^[a-zA-Z0-9]+$");
        for (int i = 0; i < 1000; i++) {
            String s = TestUtils.randomString();
            boolean condition = compile.matcher(s).find();
            assertTrue(s, condition);
        }
    }
}