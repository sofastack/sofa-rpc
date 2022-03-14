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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcInvokeContextTest {

    private static boolean oldEnableBaggage;
    private static Field   enableBaggageField;

    static {
        try {
            enableBaggageField = RpcInvokeContext.class.getDeclaredField("BAGGAGE_ENABLE");
            enableBaggageField.setAccessible(true);
            oldEnableBaggage = (boolean) enableBaggageField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void before() {
        try {
            enableBaggageField.set(null, true);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @After
    public void after() {
        try {
            enableBaggageField.set(null, oldEnableBaggage);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeContext() throws Exception {
    }

    @Test
    public void isBaggageEnable() throws Exception {
    }

    @Test
    public void getContext() throws Exception {
        RpcInvokeContext old = RpcInvokeContext.peekContext();
        try {
            if (old != null) {
                RpcInvokeContext.removeContext();
            }
            RpcInvokeContext context = RpcInvokeContext.peekContext();
            Assert.assertTrue(context == null);
            context = RpcInvokeContext.getContext();
            Assert.assertTrue(context != null);
            RpcInvokeContext.removeContext();
            Assert.assertTrue(RpcInvokeContext.peekContext() == null);

            context = new RpcInvokeContext();
            RpcInvokeContext.setContext(context);
            Assert.assertTrue(RpcInvokeContext.getContext() != null);
            Assert.assertNotEquals(RpcInvokeContext.getContext(), context);

            RpcInvokeContext.removeContext();
            Assert.assertTrue(RpcInvokeContext.peekContext() == null);
            Assert.assertTrue(context != null);

            RpcInvokeContext.removeContext();
            Assert.assertTrue(RpcInvokeContext.peekContext() == null);
        } finally {
            RpcInvokeContext.setContext(old);
        }
    }

    @Test
    public void peekContext() throws Exception {
    }

    @Test
    public void testThreadSafe() {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long start = System.currentTimeMillis();
                RpcInvokeContext.setContext(context);
                long now = System.currentTimeMillis();
                int i = 0;
                while (now - start < 100) {
                    now = System.currentTimeMillis();
                    i++;
                    RpcInvokeContext.getContext().addCustomHeader(i + "", i + "");
                    try {
                        new HashMap<>().putAll(RpcInvokeContext.getContext().getCustomHeader());
                    } catch (Exception e) {
                        System.out.println(i);
                        throw e;
                    }
                }

            }
        };

        new Thread(runnable).start();
        runnable.run();
    }

    @Test
    public void testSetContext() {
        RpcInvokeContext context = new RpcInvokeContext();
        context.setTargetGroup("target");
        context.setTargetURL("url");
        context.setTimeout(111);
        context.addCustomHeader("A", "B");
        context.put("C", "D");
        RpcInvokeContext.setContext(context);
        Assert.assertEquals(context.getTargetGroup(), RpcInvokeContext.getContext().getTargetGroup());
        Assert.assertEquals(context.getTargetURL(), RpcInvokeContext.getContext().getTargetURL());
        Assert.assertEquals(context.getTimeout(), RpcInvokeContext.getContext().getTimeout());
        Assert.assertEquals("B", RpcInvokeContext.getContext().getCustomHeader().get("A"));
        Assert.assertEquals("D", RpcInvokeContext.getContext().get("C"));
        Assert.assertTrue(context != RpcInvokeContext.getContext());
        RpcInvokeContext.removeContext();
    }

    @Test
    public void testConcurrentModify() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            RpcInvokeContext.getContext().put("" + i, "" + i);
        }

        CountDownLatch countDownLatch = new CountDownLatch(2);
        RpcInvokeContext mainContext = RpcInvokeContext.getContext();
        AtomicReference<RuntimeException> exceptionHolder = new AtomicReference<>();
        new Thread(() -> {
            countDownLatch.countDown();
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long start = System.currentTimeMillis();
            try {
                while (System.currentTimeMillis() - start < 100) {
                    RpcInvokeContext.setContext(mainContext);
                }
            } catch (RuntimeException e) {
                exceptionHolder.set(e);
                throw e;
            }

        }).start();

        Map<String, String> headers = RpcInvokeContext.getContext().getCustomHeader();
        countDownLatch.countDown();
        countDownLatch.await();
        long start = System.currentTimeMillis();
        int i = 0;
        while (System.currentTimeMillis() - start < 100) {
            if (exceptionHolder.get() != null) {
                throw exceptionHolder.get();
            }
            headers.put("" + i, "" + i);
            i++;
        }
    }

    @Test
    public void testBaggageNotNull() {
        RpcInvokeContext ctx = new RpcInvokeContext();
        ctx.putRequestBaggage(null, "value");
        Assert.assertTrue(isEmpty(ctx.getAllRequestBaggage()));
        ctx.putRequestBaggage("key", null);
        Assert.assertTrue(isEmpty(ctx.getAllRequestBaggage()));
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        map.put("key", null);
        map.put("key1", "value");
        ctx.putAllRequestBaggage(map);
        Assert.assertNotNull(ctx.getAllRequestBaggage());
        Assert.assertEquals(1, ctx.getAllRequestBaggage().size());
        ctx.putResponseBaggage(null, "value");
        Assert.assertTrue(isEmpty(ctx.getAllResponseBaggage()));
        ctx.putResponseBaggage("key", null);
        Assert.assertTrue(isEmpty(ctx.getAllResponseBaggage()));
        ctx.putAllResponseBaggage(map);
        Assert.assertNotNull(ctx.getAllResponseBaggage());
        Assert.assertEquals(1, ctx.getAllResponseBaggage().size());
    }

    private boolean isEmpty(Map<String, String> allResponseBaggage) {
        return allResponseBaggage == null || allResponseBaggage.size() == 0;
    }

    @Test
    public void testPutCustomHeader() {
        RpcInvokeContext ctx = new RpcInvokeContext();
        ctx.addCustomHeader(null, "value");
        ctx.addCustomHeader("key", null);
        ctx.addCustomHeader("key1", "value");
        Assert.assertEquals(1, ctx.getCustomHeader().size());
        Assert.assertEquals("value", ctx.getCustomHeader().get("key1"));
    }

}