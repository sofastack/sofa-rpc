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
package com.alipay.sofa.rpc.test.generic;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.codec.sofahessian.serialize.GenericCustomThrowableDeterminer;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.generic.bean.Job;
import com.alipay.sofa.rpc.test.generic.bean.People;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author xuanbei
 * @since 2016/07/28
 */
public class GenericBenchmarkTest extends ActivelyDestroyTest {
    @Test
    public void testAll() throws SofaRpcException, InterruptedException {
        try {
            // 发布服务
            ServerConfig serverConfig2 = new ServerConfig()
                .setPort(21234)
                .setCoreThreads(500)
                .setMaxThreads(500)
                .setDaemon(false);

            // C服务的服务端
            ProviderConfig<TestInterface> CProvider = new ProviderConfig<TestInterface>()
                .setInterfaceId(TestInterface.class.getName())
                .setRef(new TestClass())
                .setServer(serverConfig2);
            CProvider.export();

            // 引用服务
            ConsumerConfig<GenericService> BConsumer = new ConsumerConfig<GenericService>()
                .setInterfaceId(TestInterface.class.getName())
                .setGeneric(true)
                .setDirectUrl("bolt://127.0.0.1:21234")
                .setTimeout(3000)
                .setRetries(2);
            GenericService proxy = BConsumer.refer();

            setGenericThrowException(false);
            long[] disabledData = benchmark(proxy);
            setGenericThrowException(true);
            long[] enabledData = benchmark(proxy);
            Assert.assertEquals(disabledData[0], enabledData[0]); //count
            Assert.assertTrue(Math.abs(disabledData[1] - enabledData[1]) <= 1); //P50
            Assert.assertTrue(Math.abs(disabledData[2] - enabledData[2]) <= 1); //P90
            Assert.assertTrue(Math.abs(disabledData[3] - enabledData[3]) <= 1); //P99
        } finally {
            setGenericThrowException(false);
        }
    }

    private long[] benchmark(GenericService proxy) throws InterruptedException {
        int threadNum = 10;
        int countNum = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);

        List<Long> synchronizedList = Collections.synchronizedList(new ArrayList<>(threadNum * countNum / 2));

        for (int i = 0; i < threadNum; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < countNum; i++) {
                        long start = System.currentTimeMillis();
                        GenericObject genericObject = new GenericObject(
                                "com.alipay.sofa.rpc.test.generic.bean.People");
                        genericObject.putField("name", "Lilei");
                        genericObject.putField("job", new Job("coder"));
                        People people = new People();
                        people.setName("Lilei");
                        people.setJob(new Job("coder"));

                        // sync 调用
                        assertEquals(proxy.$invoke("hello",
                                new String[] {"com.alipay.sofa.rpc.test.generic.bean.People"},
                                new Object[] {people}), new TestClass().hello(people));

                        People peopleResult = proxy.$genericInvoke("hello",
                                new String[] {"com.alipay.sofa.rpc.test.generic.bean.People"},
                                new Object[] {genericObject}, People.class);

                        assertEquals(peopleResult, new TestClass().hello(people));

                        GenericObject result = (GenericObject) proxy.$genericInvoke("hello",
                                new String[] {"com.alipay.sofa.rpc.test.generic.bean.People"},
                                new Object[] {genericObject});
                        isCorrect(result);

                        synchronizedList.add(System.currentTimeMillis() - start);
                    }
                    countDownLatch.countDown();
                }
            }).start();
        }

        countDownLatch.await();

        Collections.sort(synchronizedList);
        int size = synchronizedList.size();
        return new long[] {synchronizedList.size(), synchronizedList.get(size / 2), synchronizedList.get(size * 9 / 10),
                synchronizedList.get(size * 99 / 100)};
    }

    private void isCorrect(GenericObject result) {
        Assert.assertEquals(result.getType(), "com.alipay.sofa.rpc.test.generic.bean.People");
        Assert.assertEquals(result.getField("name"), "Lilei");
        GenericObject genericObject = (GenericObject) result.getField("job");
        Assert.assertEquals(genericObject.getType(), "com.alipay.sofa.rpc.test.generic.bean.Job");
        Assert.assertEquals(genericObject.getField("name"), "coder");
    }

    public static void setGenericThrowException(boolean enabled) {
        try {
            Field field = GenericCustomThrowableDeterminer.class.getDeclaredField("GENERIC_THROW_EXCEPTION");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
