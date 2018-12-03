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
package com.alipay.sofa.rpc.asynchain;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.bolt.BoltSendableResponseCallback;

import java.util.Random;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServiceBImpl implements ServiceB {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceBImpl.class);

    private Random              random = new Random();

    ServiceC                    serviceC;

    public ServiceBImpl(ServiceC serviceC) {
        this.serviceC = serviceC;
    }

    @Override
    public int getInt(int num) {
        RpcInvokeContext.getContext().setResponseCallback(new BoltSendableResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                // 此时C-异步返回->B
                LOGGER.info("b get resp from c :" + appResponse);

                int respToA = random.nextInt(1000);
                // 调这个方法B-异步返回->A
                sendAppResponse(respToA);
                // 如果A是异步调用，则拿到这个appResponse返回值
            }
        });

        String s = serviceC.getStr("xx");

        return -1;
    }
}
