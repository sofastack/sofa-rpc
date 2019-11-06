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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alipay.sofa.rpc.common.RpcConstants.ADDRESS_DEFAULT_GROUP;
import static com.alipay.sofa.rpc.common.RpcConstants.ADDRESS_DIRECT_GROUP;

/**
 *
 */
public class SingleGroupAddressHolderTest {

    @Test
    public void getProviders() throws Exception {
        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);
        Assert.assertTrue(ProviderHelper.isEmpty(addressHolder.getProviderGroup(null)));
        Assert.assertTrue(ProviderHelper.isEmpty(addressHolder.getProviderGroup(StringUtils.EMPTY)));
        Assert.assertTrue(ProviderHelper.isEmpty(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP)));

        addressHolder.registryGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        addressHolder.registryGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12201"));

        Assert.assertTrue(addressHolder.getProviderGroup(null).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup(StringUtils.EMPTY).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 2);

        addressHolder.directUrlGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));

        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 1);
        Assert.assertTrue(addressHolder.getProviderGroup(null).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup(StringUtils.EMPTY).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 2);
    }

    @Test
    public void getAllProviders() throws Exception {

        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);
        List<ProviderGroup> ps = addressHolder.getProviderGroups();
        Assert.assertTrue(ps.size() == 2);
        Assert.assertTrue(ps.get(0).size() == 0);
        addressHolder.registryGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        addressHolder.registryGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12201"));

        ps = addressHolder.getProviderGroups();
        Assert.assertTrue(ps.size() == 2);
        Assert.assertTrue(ps.get(0).size() == 2);

        addressHolder.directUrlGroup.add(ProviderHelper.toProviderInfo("127.0.0.1:12200"));
        Assert.assertTrue(ps.size() == 2);
        Assert.assertTrue(ps.get(0).size() == 2);
        Assert.assertTrue(ps.get(1).size() == 1);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 3);
    }

    @Test
    public void addProvider() throws Exception {
        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);

        addressHolder.addProvider(new ProviderGroup("xxx", new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 0);

        addressHolder.addProvider(new ProviderGroup("xxx", Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 2);

        addressHolder.addProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP, new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 0);

        addressHolder.addProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP,
            Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"),
                ProviderHelper.toProviderInfo("127.0.0.1:12201"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 2);
    }

    @Test
    public void removeProvider() throws Exception {
        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);
        addressHolder.addProvider(new ProviderGroup("xxx", Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 3);

        addressHolder.removeProvider(new ProviderGroup("xxx", new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 3);

        addressHolder.removeProvider(new ProviderGroup("xxx", Collections.singletonList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 2);

        addressHolder.removeProvider(new ProviderGroup("xxx", Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 0);

        addressHolder.addProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP, Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 3);

        addressHolder.removeProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP, new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 3);

        addressHolder.removeProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP, Collections.singletonList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 2);

        addressHolder.removeProvider(new ProviderGroup(ADDRESS_DIRECT_GROUP, Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 0);

    }

    @Test
    public void updateProviders() throws Exception {
        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);

        addressHolder.updateProviders(new ProviderGroup("xxx", Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 3);

        addressHolder.updateProviders(new ProviderGroup("xxx", new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DEFAULT_GROUP).size() == 0);

        addressHolder.updateProviders(new ProviderGroup(ADDRESS_DIRECT_GROUP, Arrays.asList(
            ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 3);

        addressHolder.updateProviders(new ProviderGroup(ADDRESS_DIRECT_GROUP, new ArrayList<ProviderInfo>()));
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 0);
    }

    @Test
    public void updateAllProviders() throws Exception {
        SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);
        List<ProviderGroup> current;
        // 更新为单个
        List<ProviderGroup> ps0 = new ArrayList<ProviderGroup>();
        ps0.add(new ProviderGroup("xx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"))));
        ps0.add(new ProviderGroup(ADDRESS_DIRECT_GROUP, Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12203"))));
        addressHolder.updateAllProviders(ps0);

        current = addressHolder.getProviderGroups();
        Assert.assertTrue(current.size() == 2);
        Assert.assertTrue(current.get(0).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup("xxx").size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup(ADDRESS_DIRECT_GROUP).size() == 1);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 3);

        // 增加
        List<ProviderGroup> ps1 = new ArrayList<ProviderGroup>();
        ps1.add(new ProviderGroup("xx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"))));
        ps1.add(new ProviderGroup("yy", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12202"),
            ProviderHelper.toProviderInfo("127.0.0.1:12203"))));
        addressHolder.updateAllProviders(ps1);

        current = addressHolder.getProviderGroups();
        Assert.assertTrue(current.size() == 2);
        Assert.assertTrue(current.get(0).size() == 4);
        Assert.assertTrue(addressHolder.getProviderGroup("xxx").size() == 4);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 4);

        // 更新为空
        List<ProviderGroup> ps2 = new ArrayList<ProviderGroup>();
        addressHolder.updateAllProviders(ps2);

        current = addressHolder.getProviderGroups();
        Assert.assertTrue(current.size() == 2);
        Assert.assertTrue(current.get(0).size() == 0);
        Assert.assertTrue(addressHolder.getProviderGroup("xxx").size() == 0);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 0);

        // 更新为多个，有交叉
        List<ProviderGroup> ps3 = new ArrayList<ProviderGroup>();
        ps3.add(new ProviderGroup("xx", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12200"),
            ProviderHelper.toProviderInfo("127.0.0.1:12201"))));
        ps3.add(new ProviderGroup("yy", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12201"),
            ProviderHelper.toProviderInfo("127.0.0.1:12202"))));
        addressHolder.updateAllProviders(ps3);

        current = addressHolder.getProviderGroups();
        Assert.assertTrue(current.size() == 2);
        Assert.assertTrue(current.get(0).size() == 3);
        Assert.assertTrue(addressHolder.getProviderGroup("xxx").size() == 3);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 3);

        // 减少
        List<ProviderGroup> ps4 = new ArrayList<ProviderGroup>();
        ps4.add(new ProviderGroup("yy", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:12203"),
            ProviderHelper.toProviderInfo("127.0.0.1:12204"))));
        addressHolder.updateAllProviders(ps4);

        current = addressHolder.getProviderGroups();
        Assert.assertTrue(current.size() == 2);
        Assert.assertTrue(current.get(0).size() == 2);
        Assert.assertTrue(addressHolder.getProviderGroup("xxx").size() == 2);
        Assert.assertTrue(addressHolder.getAllProviderSize() == 2);
    }

    @Test
    public void readAndWriteLock() {
        final SingleGroupAddressHolder addressHolder = new SingleGroupAddressHolder(null);
        final Random random = new Random();
        final AtomicBoolean run = new AtomicBoolean(true);
        final AtomicBoolean error = new AtomicBoolean(false); // 有没有发生异常，例如死锁等
        final CountDownLatch latch = new CountDownLatch(1); // 出现异常 跳出等待
        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        // 不停的读取
                        addressHolder.getProviderGroups();
                    } catch (Exception e) {
                        error.set(true);
                        latch.countDown();
                    }
                }
            }
        }, "readThread");
        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        // 不停的变更
                        addressHolder.updateAllProviders(Arrays.asList(
                            new ProviderGroup("xxx",
                                Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:" + random.nextInt(65535)))),
                            new ProviderGroup(ADDRESS_DIRECT_GROUP,
                                Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1:" + random.nextInt(65535))))));
                    } catch (Exception e) {
                        error.set(true);
                        latch.countDown();
                    }
                }
            }
        }, "writeThread");
        readThread.start();
        writeThread.start();

        // 正常跑3秒 或者出异常
        try {
            latch.await(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            run.set(false);
        }
        Assert.assertFalse(error.get());
    }
}