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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class BeanIdMatchFilterTest {
    @Test
    public void testBeanIdMatch() {
        TestCustomizeFilter testCustomizeFilter = new TestCustomizeFilter();
        testCustomizeFilter.setIdRule("AAA,!BBB");
        Assert.assertEquals("AAA,!BBB", testCustomizeFilter.getIdRule());

        AbstractInterfaceConfig configA = new ProviderConfig();
        configA.setId("AAA");
        FilterInvoker filterInvokerA = new FilterInvoker(null, null, configA);

        AbstractInterfaceConfig configB = new ProviderConfig();
        configB.setId("BBB");
        FilterInvoker filterInvokerB = new FilterInvoker(null, null, configB);

        AbstractInterfaceConfig configC = new ProviderConfig();
        configC.setId("CCC");
        FilterInvoker filterInvokerC = new FilterInvoker(null, null, configC);

        Assert.assertEquals(true, testCustomizeFilter.needToLoad(filterInvokerA));
        Assert.assertEquals(false, testCustomizeFilter.needToLoad(filterInvokerB));
        Assert.assertEquals(true, testCustomizeFilter.needToLoad(filterInvokerC));

    }

    @Test
    public void testIsMatch() {
        TestCustomizeFilter testCustomizeFilter = new TestCustomizeFilter();
        Assert.assertTrue(testCustomizeFilter.isMatch(""));

        testCustomizeFilter = new TestCustomizeFilter();
        testCustomizeFilter.setIdRule("AAA,BBB");

        AbstractInterfaceConfig configA = new ProviderConfig();
        configA.setId("AAA");
        FilterInvoker filterInvokerA = new FilterInvoker(null, null, configA);
        Assert.assertEquals(true, testCustomizeFilter.needToLoad(filterInvokerA));
    }

}