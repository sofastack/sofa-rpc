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
package com.alipay.sofa.rpc.transport;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerTransportFactoryTest {
    @Test
    public void getServerTransport() throws Exception {
        ServerTransportConfig config = new ServerTransportConfig();
        config.setHost("0.0.0.0");
        config.setPort(22222);
        config.setBossThreads(1);
        config.setIoThreads(8);
        config.setBizMaxThreads(200);
        config.setBizPoolQueues(50);
        config.setDaemon(false);
        config.setContainer("test");
        ServerTransport server = ServerTransportFactory.getServerTransport(config);
        Assert.assertNotNull(server);

        boolean error = false;
        try {
            config.setContainer("testasdasd");
            ServerTransportFactory.getServerTransport(config);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

}