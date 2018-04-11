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
package com.alipay.sofa.rpc.context;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcRunningStateTest {
    @Test
    public void isShuttingDown() throws Exception {
        Assert.assertEquals(RpcRunningState.isShuttingDown(), RpcRunningState.shuttingDown);
    }

    @Test
    public void setShuttingDown() throws Exception {
        boolean old = RpcRunningState.isShuttingDown();
        try {
            RpcRunningState.setShuttingDown(true);
            Assert.assertTrue(RpcRunningState.isShuttingDown());
            RpcRunningState.setShuttingDown(false);
            Assert.assertFalse(RpcRunningState.isShuttingDown());
        } finally {
            RpcRunningState.setShuttingDown(old);
        }
    }

    @Test
    public void isUnitTestMode() throws Exception {
        Assert.assertEquals(RpcRunningState.isUnitTestMode(), RpcRunningState.unitTestMode);
    }

    @Test
    public void setUnitTestMode() throws Exception {
        boolean old = RpcRunningState.isUnitTestMode();
        try {
            RpcRunningState.setUnitTestMode(true);
            Assert.assertTrue(RpcRunningState.isUnitTestMode());
            RpcRunningState.setUnitTestMode(false);
            Assert.assertFalse(RpcRunningState.isUnitTestMode());
        } finally {
            RpcRunningState.setUnitTestMode(old);
        }
    }

    @Test
    public void isDebugMode() throws Exception {
        Assert.assertEquals(RpcRunningState.isDebugMode(), RpcRunningState.debugMode);
    }

    @Test
    public void setDebugMode() throws Exception {
        boolean old = RpcRunningState.isDebugMode();
        try {
            RpcRunningState.setDebugMode(true);
            Assert.assertTrue(RpcRunningState.isDebugMode());
            RpcRunningState.setDebugMode(false);
            Assert.assertFalse(RpcRunningState.isDebugMode());
        } finally {
            RpcRunningState.setDebugMode(old);
        }
    }

}