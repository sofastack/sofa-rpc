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
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Test;

/**
 * Triple协议的Deadline调用链集成测试
 * 注意：Triple协议需要特殊的配置和URL前缀
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TripleDeadlineChainTest extends AbstractDeadlineChainTest {

    @Override
    protected String getProtocolType() {
        return RpcConstants.PROTOCOL_TYPE_TRIPLE;
    }

    @Override
    protected int getBasePort() {
        return 25300; // Triple协议使用25300-25302端口
    }

    @Override
    protected void configureProvider(ProviderConfig<?> providerConfig, ServerConfig serverConfig,
                                     ApplicationConfig appConfig) {
        providerConfig.setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE);
    }

    @Override
    protected void configureConsumer(ConsumerConfig<?> consumerConfig, String protocol, String url,
                                     ApplicationConfig appConfig) {
        String tripleUrl = url.replace(protocol + "://", "tri://");
        consumerConfig.setDirectUrl(tripleUrl)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE);
    }

    @Test
    public void testTripleDeadlineChain() throws InterruptedException {
        doTestDeadlineChain();
    }
}
