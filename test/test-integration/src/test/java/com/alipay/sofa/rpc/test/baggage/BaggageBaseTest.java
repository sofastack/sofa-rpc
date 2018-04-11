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
package com.alipay.sofa.rpc.test.baggage;

import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public abstract class BaggageBaseTest extends ActivelyDestroyTest {

    @Test
    public void test() throws Exception {
        String old = System.getProperty(RpcOptions.INVOKE_BAGGAGE_ENABLE);
        RpcInvokeContext oldCtx = RpcInvokeContext.peekContext();
        boolean enable = RpcInvokeContext.isBaggageEnable();
        Field field = RpcInvokeContext.class.getDeclaredField("BAGGAGE_ENABLE");
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);//fianl标志位置0  
        field.setAccessible(true);

        try {
            field.set(null, true);
            doTest();
        } finally {
            if (old == null) {
                System.clearProperty(RpcOptions.INVOKE_BAGGAGE_ENABLE);
            } else {
                System.setProperty(RpcOptions.INVOKE_BAGGAGE_ENABLE, old);
            }
            RpcInvokeContext.setContext(oldCtx);
            field.set(null, enable);
        }
    }

    abstract void doTest() throws Exception;
}
