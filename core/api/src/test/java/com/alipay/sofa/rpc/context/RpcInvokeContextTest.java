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
public class RpcInvokeContextTest {
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
            Assert.assertEquals(RpcInvokeContext.getContext(), context);

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

}