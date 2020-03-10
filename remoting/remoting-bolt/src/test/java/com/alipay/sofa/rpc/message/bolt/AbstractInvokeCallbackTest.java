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
package com.alipay.sofa.rpc.message.bolt;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AbstractInvokeCallbackTest {

    @Test
    public void testRecordClientElapseTime() {
        BoltInvokerCallback invokerCallback = new BoltInvokerCallback(null, null,
            null, null, null, null);
        invokerCallback.recordClientElapseTime();
        Long elapse = (Long) RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE);
        Assert.assertNull(elapse);

        RpcInternalContext context = RpcInternalContext.getContext();
        invokerCallback = new BoltInvokerCallback(null, null,
            null, null, context, null);
        invokerCallback.recordClientElapseTime();
        elapse = (Long) context.getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE);
        Assert.assertNull(elapse);

        context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME, RpcRuntimeContext.now() - 1000);
        invokerCallback.recordClientElapseTime();
        elapse = (Long) context.getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE);
        Assert.assertNotNull(elapse);
    }
}