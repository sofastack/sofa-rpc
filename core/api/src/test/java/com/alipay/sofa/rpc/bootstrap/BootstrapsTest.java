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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BootstrapsTest {
    @Test
    public void from() throws Exception {
        ProviderConfig providerConfig = new ProviderConfig().setBootstrap("test");
        ProviderBootstrap bootstrap = Bootstraps.from(providerConfig);
        Assert.assertEquals(TestProviderBootstrap.class, bootstrap.getClass());
        Assert.assertEquals(providerConfig, bootstrap.getProviderConfig());
    }

    @Test
    public void from1() throws Exception {
        ConsumerConfig consumerConfig = new ConsumerConfig().setBootstrap("test");
        ConsumerBootstrap bootstrap = Bootstraps.from(consumerConfig);
        Assert.assertEquals(TestConsumerBootstrap.class, bootstrap.getClass());
        Assert.assertEquals(consumerConfig, bootstrap.getConsumerConfig());
    }

}