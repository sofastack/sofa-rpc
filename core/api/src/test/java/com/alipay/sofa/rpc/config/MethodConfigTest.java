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
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MethodConfigTest {
    @Test
    public void setParameter() throws Exception {
        MethodConfig methodConfig = new MethodConfig();
        Assert.assertNull(methodConfig.parameters);
        Assert.assertNull(methodConfig.getParameter("aa"));
        methodConfig.setName("echo");
        methodConfig.setParameter("aa", "11");
        Assert.assertNotNull(methodConfig.parameters);
        Assert.assertEquals(methodConfig.getParameter("aa"), "11");
    }

    @Test
    public void getParameter() throws Exception {
    }

}