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
package com.alipay.sofa.rpc.test.baggage;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.bolt.BoltSendableResponseCallback;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author zhanggeng
 */
public class BAsyncChainSampleServiceImpl implements SampleService {

    private final static Logger LOGGER = LoggerFactory.getLogger(BAsyncChainSampleServiceImpl.class);

    private SampleService       sampleServiceC;

    private SampleService       sampleServiceD;

    private String              reqBaggage;

    public BAsyncChainSampleServiceImpl(SampleService sampleServiceC, SampleService sampleServiceD) {
        this.sampleServiceC = sampleServiceC;
        this.sampleServiceD = sampleServiceD;
    }

    @Override
    public String hello() {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        LOGGER.info("--b1---:" + context);
        // 读取一定要在这里读取
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        context.putResponseBaggage("respBaggageB_useful1", "在返A之前写入有用");
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            RpcInvokeContext.getContext().setResponseCallback(new BoltSendableResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    // 返回一定要写在这里
                    RpcInvokeContext context = RpcInvokeContext.getContext();
                    LOGGER.info("--b3---:" + context);
                    if (reqBaggage != null) {
                        context.putResponseBaggage("respBaggageB", "b2aaa");
                    } else {
                        context.putResponseBaggage("respBaggageB_force", "b2aaaff");
                    }

                    String s1 = (String) appResponse;
                    String reqBaggageD = context.getRequestBaggage("reqBaggageD"); // 这里已经取不到值了
                    LOGGER.info("----reqBaggageD---:" + reqBaggageD);
                    String s2 = sampleServiceD.hello();
                    sendAppResponse(s1 + s2);
                    LOGGER.info("--b4---:" + RpcInvokeContext.getContext());
                    context.putResponseBaggage("respBaggageB_useless2", "在返A之前写后没用"); // 返回写在这里可能没用
                    latch.countDown();
                }
            });
            sampleServiceC.hello();
            context.putResponseBaggage("respBaggageB_useful2", "在返A之前写入有用");
            LOGGER.info("--b2---:" + RpcInvokeContext.getContext());
            latch.await(5000, TimeUnit.MILLISECONDS); // 模拟Callback更早回来的行为
            context.putResponseBaggage("respBaggageB_useless2", "在返A之前写后没用"); // 返回写在这里可能没用
            LOGGER.info("--b3---:" + RpcInvokeContext.getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EchoResponse echoObj(final EchoRequest req) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        LOGGER.info("--b1---:" + context);
        // 读取一定要在这里读取
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        context.putResponseBaggage("respBaggageB_useful1", "在返A之前写入有用");
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            RpcInvokeContext.getContext().setResponseCallback(new BoltSendableResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    // 返回一定要写在这里
                    RpcInvokeContext context = RpcInvokeContext.getContext();
                    LOGGER.info("--b3---:" + context);
                    if (reqBaggage != null) {
                        context.putResponseBaggage("respBaggageB", "b2aaa");
                    } else {
                        context.putResponseBaggage("respBaggageB_force", "b2aaaff");
                    }

                    EchoResponse s1 = (EchoResponse) appResponse;
                    String reqBaggageD = context.getRequestBaggage("reqBaggageD"); // 这里已经取不到值了
                    LOGGER.info("----reqBaggageD---:" + reqBaggageD);
                    EchoResponse s2 = sampleServiceD.echoObj(req);
                    sendAppResponse(EchoResponse.newBuilder().setCode(200)
                        .setMessage(s1.getMessage() + s2.getMessage()).build());
                    LOGGER.info("--b4---:" + RpcInvokeContext.getContext());
                    context.putResponseBaggage("respBaggageB_useless2", "在返A之前写后没用"); // 返回写在这里可能没用
                    latch.countDown();
                }
            });
            sampleServiceC.echoObj(req);
            context.putResponseBaggage("respBaggageB_useful2", "在返A之前写入有用");
            LOGGER.info("--b2---:" + RpcInvokeContext.getContext());
            latch.await(5000, TimeUnit.MILLISECONDS); // 模拟Callback更早回来的行为
            context.putResponseBaggage("respBaggageB_useless2", "在返A之前写后没用"); // 返回写在这里可能没用
            LOGGER.info("--b3---:" + RpcInvokeContext.getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getReqBaggage() {
        return reqBaggage;
    }
}
