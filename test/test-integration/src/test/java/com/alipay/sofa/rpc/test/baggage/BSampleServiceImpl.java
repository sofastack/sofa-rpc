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
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BSampleServiceImpl implements SampleService {

    private SampleService sampleServiceC;

    private SampleService sampleServiceD;

    private String        reqBaggage;

    public BSampleServiceImpl(SampleService sampleServiceC, SampleService sampleServiceD) {
        this.sampleServiceC = sampleServiceC;
        this.sampleServiceD = sampleServiceD;
    }

    @Override
    public String hello() {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("-----b-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageB", "b2aaa");
        } else {
            context.putResponseBaggage("respBaggageB_force", "b2aaaff");
        }
        String s1 = sampleServiceC.hello();
        String s2 = sampleServiceD.hello();
        return s1 + s2;
    }

    @Override
    public EchoResponse echoObj(EchoRequest req) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("-----b-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageB");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageB", "b2aaa");
        } else {
            context.putResponseBaggage("respBaggageB_force", "b2aaaff");
        }
        EchoResponse s1 = sampleServiceC.echoObj(req);
        EchoResponse s2 = sampleServiceD.echoObj(req);
        return EchoResponse.newBuilder().setCode(200).setMessage(s1.getMessage() + s2.getMessage()).build();
    }

    public String getReqBaggage() {
        return reqBaggage;
    }
}
