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
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author zhanggeng
 */
public class BCallbackSampleServiceImpl implements SampleService {

    private SampleService sampleServiceC;

    private SampleService sampleServiceD;

    private String        reqBaggage;

    public BCallbackSampleServiceImpl(SampleService sampleServiceC, SampleService sampleServiceD) {
        this.sampleServiceC = sampleServiceC;
        this.sampleServiceD = sampleServiceD;
    }

    @Override
    public String hello() {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("--b1-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageB", "b2aaa");
        } else {
            context.putResponseBaggage("respBaggageB_force", "b2aaaff");
        }
        final String[] str = new String[2];
        final CountDownLatch latch = new CountDownLatch(2);
        try {
            RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    str[0] = (String) appResponse;
                    latch.countDown();
                }

                @Override
                public void onAppException(Throwable throwable, String methodName,
                                           RequestBase request) {
                    latch.countDown();
                }

                @Override
                public void onSofaException(SofaRpcException sofaException, String methodName,
                                            RequestBase request) {
                    latch.countDown();
                }
            });
            sampleServiceC.hello();
            RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    str[1] = (String) appResponse;
                    latch.countDown();
                }

                @Override
                public void onAppException(Throwable throwable, String methodName,
                                           RequestBase request) {
                    latch.countDown();
                }

                @Override
                public void onSofaException(SofaRpcException sofaException, String methodName,
                                            RequestBase request) {
                    latch.countDown();
                }
            });
            sampleServiceD.hello();
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str[0] + str[1];
    }

    @Override
    public EchoResponse echoObj(EchoRequest req) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("--b1-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageB", "b2aaa");
        } else {
            context.putResponseBaggage("respBaggageB_force", "b2aaaff");
        }
        final EchoResponse[] str = new EchoResponse[2];
        final CountDownLatch latch = new CountDownLatch(2);
        try {
            RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    str[0] = (EchoResponse) appResponse;
                    latch.countDown();
                }

                @Override
                public void onAppException(Throwable throwable, String methodName,
                                           RequestBase request) {
                    latch.countDown();
                }

                @Override
                public void onSofaException(SofaRpcException sofaException, String methodName,
                                            RequestBase request) {
                    latch.countDown();
                }
            });
            sampleServiceC.hello();
            RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    str[1] = (EchoResponse) appResponse;
                    latch.countDown();
                }

                @Override
                public void onAppException(Throwable throwable, String methodName,
                                           RequestBase request) {
                    latch.countDown();
                }

                @Override
                public void onSofaException(SofaRpcException sofaException, String methodName,
                                            RequestBase request) {
                    latch.countDown();
                }
            });
            sampleServiceD.hello();
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EchoResponse s1 = str[0];
        EchoResponse s2 = str[1];
        return EchoResponse.newBuilder().setCode(200).setMessage(s1.getMessage() + s2.getMessage()).build();
    }

    public String getReqBaggage() {
        return reqBaggage;
    }
}
