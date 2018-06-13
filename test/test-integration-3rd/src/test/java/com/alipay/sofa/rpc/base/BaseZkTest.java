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
package com.alipay.sofa.rpc.base;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * @author bystander
 * @version $Id: BaseZkTest.java, v 0.1 2018年05月22日 7:55 PM bystander Exp $
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

    /**
     * 循环获取某个值，直到满足预期或者超时为止，相比sleep可高效
     * @param callable 任务
     * @param expect 期望值
     * @param period 周期
     * @param times 次数
     * @param <T> 值类型
     * @return 返回值
     */
    protected <T> T delayGet(Callable<T> callable, T expect, int period, int times) {
        T result = null;
        int i = 0;
        while (i++ < times) {
            try {
                Thread.sleep(period);//第一个窗口结束
                result = callable.call();
                if (result != null && result.equals(expect)) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}