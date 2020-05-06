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
package com.alipay.sofa.rpc.bootstrap.bolt;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.alipay.sofa.rpc.bootstrap.Bootstraps;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

/**
 *
 *
 * @author <a href="mailto:njucoder@gmail.com">Min Li</a>
 */
public class BoltClientProxyInvokerTest {
    @Test
    public void testParseSerializeType() throws Exception {
        ConsumerConfig consumerConfig = new ConsumerConfig().setProtocol("bolt");
        ConsumerBootstrap bootstrap = Bootstraps.from(consumerConfig);
        BoltClientProxyInvoker invoker = new BoltClientProxyInvoker(bootstrap);
        byte actual = invoker.parseSerializeType(RpcConstants.SERIALIZE_HESSIAN2);
        assertEquals(RemotingConstants.SERIALIZE_CODE_HESSIAN, actual);
    }

    @Test(expected = SofaRpcRuntimeException.class)
    public void testParseSerializeTypeException() {
        ConsumerConfig consumerConfig = new ConsumerConfig().setProtocol("bolt");
        ConsumerBootstrap bootstrap = Bootstraps.from(consumerConfig);
        BoltClientProxyInvoker invoker = new BoltClientProxyInvoker(bootstrap);
        invoker.parseSerializeType("unknown");
    }
}