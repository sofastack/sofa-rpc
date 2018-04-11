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
package com.alipay.sofa.rpc.transmit;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TransmitConfigHelperTest {
    @Test
    public void parseTransmitConfig() throws Exception {

        String url = "1.1.1.1";
        TransmitConfig config = TransmitConfigHelper.parseTransmitConfig("aaa", url);
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getAddress(), "1.1.1.1");
        Assert.assertEquals(config.getDuring(), 0);

        url = "weightStarting:0.7,during:120,weightStarted:0.2,address:1.1.1.1";
        config = TransmitConfigHelper.parseTransmitConfig("aaa", url);
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getAddress(), "1.1.1.1");
        Assert.assertEquals(config.getDuring(), 120000);
        Assert.assertTrue(config.getWeightStarted() == 0.2d);
        Assert.assertTrue(config.getWeightStarting() == 0.7d);
        Assert.assertTrue(config.getTransmitTimeout() == 10000);
    }

}
