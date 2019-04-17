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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author bystander
 * @version : DynamicFacotryTest.java, v 0.1 2019年04月12日 12:08 bystander Exp $
 */
public class DynamicFacotryTest {

    @Test
    public void test() {
        DynamicConfigManager dynamicManager = DynamicConfigManagerFactory.getDynamicManager("testApp", "simple");
        Assert.assertNotNull(dynamicManager);

        final String service = "com.alipay.sofa.rpc.demo.HelloService:1.0";

        dynamicManager.initServiceConfiguration(service);

        String fetchValue = dynamicManager.getConsumerMethodProperty(service, "methodName", "timeout");

        Assert.assertEquals("1000", fetchValue);
    }
}