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
import java.util.Set;

public class UserThreadPoolManagerTest {
    @Test
    public void getUserThreadPoolMap() {

        Set<UserThreadPool> userThreadPoolSet = UserThreadPoolManager.getUserThreadPoolSet();
        Assert.assertTrue(userThreadPoolSet.isEmpty());

        UserThreadPool pool1 = new UserThreadPool();
        Assert.assertTrue(pool1.getThreadPoolName().startsWith(UserThreadPool.DEFAUT_POOL_NAME + "-"));

        UserThreadPoolManager.registerUserThread("service1", pool1);
        userThreadPoolSet = UserThreadPoolManager.getUserThreadPoolSet();
        Assert.assertEquals(1, userThreadPoolSet.size());

        UserThreadPool pool2 = new UserThreadPool();
        Assert.assertTrue(pool2.getThreadPoolName().startsWith(UserThreadPool.DEFAUT_POOL_NAME + "-"));
        Assert.assertNotEquals(pool2.getThreadPoolName(), pool1.getThreadPoolName());

        UserThreadPoolManager.registerUserThread("service2", pool2);
        userThreadPoolSet = UserThreadPoolManager.getUserThreadPoolSet();
        Assert.assertEquals(2, userThreadPoolSet.size());

        UserThreadPool pool3 = new UserThreadPool("samePoolName");
        UserThreadPoolManager.registerUserThread("service3", pool3);
        UserThreadPool pool4 = new UserThreadPool("samePoolName");
        UserThreadPoolManager.registerUserThread("service4", pool4);
        userThreadPoolSet = UserThreadPoolManager.getUserThreadPoolSet();
        Assert.assertEquals(4, userThreadPoolSet.size());
    }
}