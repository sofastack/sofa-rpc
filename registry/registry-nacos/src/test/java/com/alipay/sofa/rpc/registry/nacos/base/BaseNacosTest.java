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
package com.alipay.sofa.rpc.registry.nacos.base;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import name.jervyshi.nacos.NacosProcess;
import name.jervyshi.nacos.NacosStarterBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * The type Base nacos test.
 * @author <a href=mailto:jervyshi@gmail.com>JervyShi</a>
 */
public abstract class BaseNacosTest {

    protected final static Logger LOGGER = LoggerFactory.getLogger(BaseNacosTest.class);

    protected NacosProcess        nacosProcess;

    /**
     * Ad before class.
     */
    @BeforeClass
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);
    }

    /**
     * Ad after class.
     */
    @AfterClass
    public static void adAfterClass() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Before
    public void setup() {
        //see https://nacos.io/zh-cn/docs/2.0.0-compatibility.html
        nacosProcess = NacosStarterBuilder.nacosStarter().withServerPort(8848).withNacosVersion("2.2.3").build()
            .start();
    }

    @After
    public void cleanup() throws Exception {
        nacosProcess.close();
    }
}
