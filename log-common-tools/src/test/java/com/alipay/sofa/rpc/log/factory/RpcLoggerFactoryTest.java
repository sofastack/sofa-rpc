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
package com.alipay.sofa.rpc.log.factory;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcLoggerFactoryTest {
    @Test
    public void getLogger() throws Exception {

        Assert.assertNull(RpcLoggerFactory.getLogger(null, "appname1"));
        Logger logger1 = RpcLoggerFactory.getLogger("xxx", "appname1");
        Assert.assertNotNull(logger1);
        Logger logger2 = RpcLoggerFactory.getLogger("xxx", "appname1");
        Assert.assertNotNull(logger1);
        Assert.assertEquals(logger1, logger2);
        Logger logger3 = RpcLoggerFactory.getLogger("xxx", "appname2");
        Assert.assertFalse(logger1.equals(logger3));
    }

}