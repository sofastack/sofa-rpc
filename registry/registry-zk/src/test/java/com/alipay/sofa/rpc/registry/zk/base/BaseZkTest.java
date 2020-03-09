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
package com.alipay.sofa.rpc.registry.zk.base;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * if you use zk to be registry ,your test case must be extends this class
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public abstract class BaseZkTest {
    protected static TestingServer server = null;

    @BeforeClass
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);

        try {
            server = new TestingServer(2181, true);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void adAfterClass() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();

        if (server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}