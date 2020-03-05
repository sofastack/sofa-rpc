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
package com.alipay.sofa.rpc.client.aft;

import com.alipay.sofa.rpc.client.aft.bean.FaultHelloService;
import com.alipay.sofa.rpc.client.aft.bean.HelloServiceTimeOutImpl;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import org.junit.After;
import org.junit.Before;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public abstract class FaultBaseServiceTest extends FaultBaseTest {

    protected FaultHelloService helloService;

    @Before
    public void beforeMethod() throws Exception {
        providerConfig.setRef(new HelloServiceTimeOutImpl());
        providerConfig.export();
        // test reuse client transport
        consumerConfigNotUse.refer();
        helloService = consumerConfig.refer();
    }

    @After
    public void afterMethod() {
        providerConfig.unExport();
        consumerConfigNotUse.unRefer();
        consumerConfig.unRefer();
        consumerConfig = null;
        consumerConfig2 = null;
        consumerConfigAnotherApp = null;
        helloService = null;

        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

}