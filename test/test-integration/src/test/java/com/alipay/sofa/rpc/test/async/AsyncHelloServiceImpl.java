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
package com.alipay.sofa.rpc.test.async;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.bolt.BoltSendableResponseCallback;
import com.alipay.sofa.rpc.test.HelloService;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AsyncHelloServiceImpl implements AsyncHelloService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHelloServiceImpl.class);

    private HelloService        helloService;

    public AsyncHelloServiceImpl(HelloService helloService) {
        this.helloService = helloService;
    }

    @Override
    public String sayHello(String name, int age) {
        LOGGER.info("[2]----B get req :{}, {}", name, age);
        // 模拟A-->B-->C场景
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.setTimeout(2000);
        context.setResponseCallback(new BoltSendableResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                // 此时C-异步返回->B
                LOGGER.info("[5]----B get result: {}", appResponse);
                // 调这个方法B-异步返回->A
                sendAppResponse(appResponse);
                // 如果A是异步调用，则拿到这个appResponse返回值
            }
        });

        String c0 = helloService.sayHello(name, age); // B-异步调用->C
        if (c0 != null) {
            LOGGER.error("--------c0 is not null");
        }
        return "hello async无效返回"; // 如果设置了AsyncProxyResponseCallback，则此处返回其实是无效。
    }

    @Override
    public String appException(String name) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.setTimeout(2000);
        context.setResponseCallback(new BoltSendableResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                sendAppException(new RuntimeException("1234"));
            }
        });

        helloService.sayHello(name, 1); // B-异步调用->C
        return null;
    }

    @Override
    public String rpcException(String name) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.setTimeout(2000);
        context.setResponseCallback(new BoltSendableResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                sendSofaException(new SofaRpcException(RpcErrorType.SERVER_BUSY, "bbb"));
            }
        });

        helloService.sayHello(name, 1); // B-异步调用->C
        return null;
    }
}
