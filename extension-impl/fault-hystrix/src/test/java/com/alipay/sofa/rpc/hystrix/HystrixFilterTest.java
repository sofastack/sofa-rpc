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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;

@ClassPathExclusions("hystrix-core")
@RunWith(ModifiedClassPathRunner.class)
public class HystrixFilterTest {

    @Test
    public void testNeedToLoadWhenHystrixNotInClassPath() {
        RpcConfigs.putValue(HystrixConstants.SOFA_HYSTRIX_ENABLED, "true");
        FilterInvoker filterInvoker = new FilterInvoker(null, null, new ConsumerConfig());
        Assert.assertFalse(new HystrixFilter().needToLoad(filterInvoker));
    }
}
