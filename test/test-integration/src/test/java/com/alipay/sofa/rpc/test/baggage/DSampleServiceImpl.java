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
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class DSampleServiceImpl implements SampleService {

    private String reqBaggage;

    @Override
    public String hello() {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("----d-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageD");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageD", "d2aaa");
        } else {
            context.putResponseBaggage("respBaggageD_force", "d2aaaff");
        }
        return "hello world d";
    }

    @Override
    public EchoResponse echoObj(EchoRequest req) {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        System.out.println("----d-----:" + context);
        reqBaggage = context.getRequestBaggage("reqBaggageD");
        if (reqBaggage != null) {
            context.putResponseBaggage("respBaggageD", "d2aaa");
        } else {
            context.putResponseBaggage("respBaggageD_force", "d2aaaff");
        }
        return EchoResponse.newBuilder().setCode(200).setMessage("hello world d").build();
    }

    public String getReqBaggage() {
        return reqBaggage;
    }
}
