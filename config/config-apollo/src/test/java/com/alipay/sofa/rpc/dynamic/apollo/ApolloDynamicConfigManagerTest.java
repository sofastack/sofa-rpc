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
package com.alipay.sofa.rpc.dynamic.apollo;

import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManagerFactory;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;

public class ApolloDynamicConfigManagerTest {

    private final static Logger logger = LoggerFactory
            .getLogger(ApolloDynamicConfigManagerTest.class);

    private DynamicConfigManager apolloDynamicConfigManager = DynamicConfigManagerFactory.getDynamicManager("test",
            "apollo");

    @Test
    public void getProviderServiceProperty() {
        String result = apolloDynamicConfigManager.getProviderServiceProperty("serviceName", "timeout");
        Assert.assertEquals(DynamicHelper.DEFAULT_DYNAMIC_VALUE, result);
    }

    @Test
    public void getConsumerServiceProperty() {
        String result = apolloDynamicConfigManager.getConsumerServiceProperty("serviceName", "timeout");
        Assert.assertEquals(DynamicHelper.DEFAULT_DYNAMIC_VALUE, result);
    }

    @Test
    public void getProviderMethodProperty() {
        String result = apolloDynamicConfigManager.getProviderMethodProperty("serviceName", "methodName", "timeout");
        Assert.assertEquals(DynamicHelper.DEFAULT_DYNAMIC_VALUE, result);
    }

    @Test
    public void getConsumerMethodProperty() {
        String result = apolloDynamicConfigManager.getConsumerMethodProperty("serviceName", "methodName", "timeout");
        Assert.assertEquals(DynamicHelper.DEFAULT_DYNAMIC_VALUE, result);
    }
}