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
package com.alipay.sofa.rpc.test.deadline;

import com.alipay.sofa.rpc.common.RpcConstants;
import org.junit.Test;

/**
 * Bolt协议的Deadline调用链集成测试
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltDeadlineChainTest extends AbstractDeadlineChainTest {

    @Override
    protected String getProtocolType() {
        return RpcConstants.PROTOCOL_TYPE_BOLT;
    }

    @Override
    protected int getBasePort() {
        return 22300; // Bolt协议使用22300-22302端口
    }

    @Test
    public void testBoltDeadlineChain() throws InterruptedException {
        doTestDeadlineChain();
    }
}
