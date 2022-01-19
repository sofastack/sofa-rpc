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

import com.alipay.sofa.rpc.server.UserThreadPool;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;

public class UserThreadPoolManagerTest {
    @Test
    public void getUserThreadPoolMap() {

        Map<String, UserThreadPool> userThreadPoolMap = UserThreadPoolManager.getUserThreadPoolMap();
        Assert.assertTrue(userThreadPoolMap.size() == 0);

        UserThreadPool pool1 = new UserThreadPool();
        Assert.assertEquals(pool1.getThreadPoolName(), UserThreadPool.DEFAUT_POOL_NAME + "-0");

        UserThreadPoolManager.registerUserThread("service1", pool1);
        userThreadPoolMap = UserThreadPoolManager.getUserThreadPoolMap();
        Assert.assertTrue(userThreadPoolMap.size() == 1);

        UserThreadPool pool2 = new UserThreadPool();
        Assert.assertEquals(pool2.getThreadPoolName(), UserThreadPool.DEFAUT_POOL_NAME + "-1");

        UserThreadPoolManager.registerUserThread("service2", pool2);
        userThreadPoolMap = UserThreadPoolManager.getUserThreadPoolMap();
        Assert.assertTrue(userThreadPoolMap.size() == 2);

    }
}