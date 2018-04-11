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
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class JAXRSProviderManagerTest {

    @Test
    public void testAll() {

        Integer integer_10 = new Integer(10);
        String sss = "SSS";
        String aaa = "AAA";

        JAXRSProviderManager.registerCustomProviderInstance(integer_10);
        JAXRSProviderManager.registerCustomProviderInstance(sss);

        JAXRSProviderManager.registerInternalProviderClass(integer_10.getClass());
        JAXRSProviderManager.registerInternalProviderClass(aaa.getClass());
        JAXRSProviderManager.registerInternalProviderClass(sss.getClass());

        Assert.assertEquals(true, JAXRSProviderManager.getCustomProviderInstances().contains(integer_10));
        Assert.assertEquals(true, JAXRSProviderManager.getCustomProviderInstances().contains(sss));
        Assert.assertEquals(true, JAXRSProviderManager.getInternalProviderClasses().contains(integer_10.getClass()));
        Assert.assertEquals(true, JAXRSProviderManager.getInternalProviderClasses().contains(sss.getClass()));
        Assert.assertEquals(true, JAXRSProviderManager.getInternalProviderClasses().contains(aaa.getClass()));

        JAXRSProviderManager.removeInternalProviderClass(aaa.getClass());
        Assert.assertEquals(false, JAXRSProviderManager.getInternalProviderClasses().contains(sss.getClass()));
        Assert.assertEquals(false, JAXRSProviderManager.getInternalProviderClasses().contains(aaa.getClass()));

        JAXRSProviderManager.removeInternalProviderClass(integer_10.getClass());
        JAXRSProviderManager.removeCustomProviderInstance(integer_10);
        JAXRSProviderManager.removeCustomProviderInstance(sss);

    }
}