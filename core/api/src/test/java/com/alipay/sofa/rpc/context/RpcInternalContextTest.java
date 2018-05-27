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

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.message.ResponseFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcInternalContextTest {

    @Before
    public void before() {
        RpcInternalContext.removeAllContext();
    }

    @After
    public void after() {
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void testPop() {
        RpcInternalContext.pushContext();

        RpcInternalContext.popContext();

        RpcInternalContext current = RpcInternalContext.peekContext();
        Assert.assertNull(current);

        RpcInternalContext parentCtx = RpcInternalContext.getContext(); // 生成一个
        Assert.assertNotNull(parentCtx);
        parentCtx.setRemoteAddress("127.0.0.1", 12200);

        Assert.assertEquals(RpcInternalContext.getContext(), parentCtx);
        Assert.assertEquals(RpcInternalContext.getContext().getRemoteAddress().toString(), "127.0.0.1:12200");

        RpcInternalContext.pushContext(); // push进去后，当前为空

        current = RpcInternalContext.peekContext();
        Assert.assertNull(current);

        Assert.assertFalse(parentCtx.equals(RpcInternalContext.getContext()));
        Assert.assertNull(RpcInternalContext.getContext().getRemoteAddress());
        RpcInternalContext.removeContext();

        current = RpcInternalContext.peekContext(); // 删掉后，当前为空
        Assert.assertNull(current);

        RpcInternalContext.popContext(); // pop一个出来

        current = RpcInternalContext.getContext();
        Assert.assertNotNull(current);

        Assert.assertEquals(RpcInternalContext.getContext(), parentCtx);
        Assert.assertEquals(RpcInternalContext.getContext().getRemoteAddress().toString(), "127.0.0.1:12200");
    }

    @Test
    public void testAddress() {
        RpcInternalContext context = RpcInternalContext.getContext();
        context.setLocalAddress(null, 80);
        context.setLocalAddress("127.0.0.1", -1);
        context.setRemoteAddress(null, 80);
        context.setRemoteAddress("127.0.0.1", -1);
        Assert.assertTrue(context.getRemoteAddress().getPort() == 0);
        Assert.assertEquals("127.0.0.1", context.getRemoteHostName());
    }

    @Test
    public void testAttachment() {
        Assert.assertTrue(RpcInternalContext.isAttachmentEnable());
        RpcInternalContext context = RpcInternalContext.getContext();
        boolean error = false;
        try {
            context.setAttachment("1", "1");
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");
        context.setAttachments(map);
        Assert.assertEquals("1111", context.getAttachment("_11"));
        context.setAttachment(null, "22222");
        context.setAttachment("_22", null);
        Assert.assertNull(context.getAttachment(null));
        Assert.assertNull(context.getAttachment("_22"));

        Assert.assertNull(context.removeAttachment("_33"));
        Assert.assertEquals("3333", context.removeAttachment(".33"));

        context.clearAttachments();
        Assert.assertNull(context.removeAttachment("11"));
    }

    @Test
    public void testClear() {
        RpcInternalContext context = RpcInternalContext.getContext();
        context.setRemoteAddress("127.0.0.1", 1234);
        context.setLocalAddress("127.0.0.1", 2345);
        context.setFuture(new ResponseFuture<String>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
                return null;
            }

            @Override
            public ResponseFuture addListeners(List<SofaResponseCallback> sofaResponseCallbacks) {
                return null;
            }

            @Override
            public ResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
                return null;
            }
        });

        context.setProviderInfo(ProviderHelper.toProviderInfo("127.0.0.1:80"));
        context.setAttachment("_xxxx", "yyyy");

        context.clear();
        Assert.assertNull(context.getRemoteAddress());
        Assert.assertNull(context.getLocalAddress());
        Assert.assertNull(context.getFuture());
        Assert.assertFalse(context.isProviderSide());
        Assert.assertFalse(context.isConsumerSide());
        Assert.assertNull(context.getProviderInfo());
        Assert.assertTrue(context.getAttachments().isEmpty());
        Assert.assertNotNull(context.getStopWatch());
        Assert.assertTrue(context.getStopWatch().read() == 0);

        Assert.assertNotNull(context.toString());
    }

    @Test
    public void testKey() {
        Assert.assertTrue(RpcInternalContext.isValidInternalParamKey("."));
        Assert.assertTrue(RpcInternalContext.isValidInternalParamKey(".xx"));
        Assert.assertTrue(RpcInternalContext.isValidInternalParamKey("_"));
        Assert.assertTrue(RpcInternalContext.isValidInternalParamKey("_xx"));
        Assert.assertFalse(RpcInternalContext.isHiddenParamKey("aaaa"));

        Assert.assertTrue(RpcInternalContext.isHiddenParamKey("."));
        Assert.assertTrue(RpcInternalContext.isHiddenParamKey(".xx"));
        Assert.assertFalse(RpcInternalContext.isHiddenParamKey("_"));
        Assert.assertFalse(RpcInternalContext.isHiddenParamKey("_xx"));
        Assert.assertFalse(RpcInternalContext.isHiddenParamKey("aaaa"));
    }
}