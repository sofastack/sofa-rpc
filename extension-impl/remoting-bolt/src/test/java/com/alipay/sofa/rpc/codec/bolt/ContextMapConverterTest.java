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
package com.alipay.sofa.rpc.codec.bolt;

import com.alipay.sofa.rpc.common.RemotingConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ContextMapConverterTest {
    @Test
    public void flatCopyTo() throws Exception {

        Map<String, Object> requestProps = new HashMap<String, Object>();
        requestProps.put("xx", "xxxxxxx");
        requestProps.put("yyy", new String[] { "yyyy" }); // string数组无法传递
        requestProps.put("zzzz", 333);

        Map<String, String> header = new HashMap<String, String>();
        Map<String, String> context = new HashMap<String, String>();
        context.put("sofaCallerApp", "test");
        context.put("sofaCallerIp", "10.15.233.63");
        context.put("sofaPenAttrs", "");
        context.put("sofaRpcId", "0");
        context.put("sofaTraceId", "0a0fe93f1488349732342100153695");
        context.put("sysPenAttrs", "");
        context.put("penAttrs", "Hello=world&");
        requestProps.put(RemotingConstants.RPC_TRACE_NAME, context);

        Map<String, String> requestBaggage = new HashMap<String, String>();
        requestBaggage.put("aaa", "reqasdhjaksdhaksdyiasdhasdhaskdhaskd");
        requestBaggage.put("bbb", "req10.15.233.63");
        requestBaggage.put("ccc", "reqwhat 's wrong");
        requestProps.put(RemotingConstants.RPC_REQUEST_BAGGAGE, requestBaggage);

        Map<String, String> responseBaggage = new HashMap<String, String>();
        responseBaggage.put("xxx", "respasdhjaksdhaksdyiasdhasdhaskdhaskd");
        responseBaggage.put("yyy", "resp10.15.233.63");
        responseBaggage.put("zzz", "resphehehe");
        requestProps.put(RemotingConstants.RPC_RESPONSE_BAGGAGE, responseBaggage);

        //        rpcSerialization.
        ContextMapConverter.flatCopyTo("", requestProps, header);
        Assert.assertTrue(header.size() == 15);

        for (Map.Entry<String, String> entry : header.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("");

        Map<String, Object> newRequestProps = new HashMap<String, Object>();

        Map<String, String> newContext = new HashMap();
        ContextMapConverter.treeCopyTo(RemotingConstants.RPC_TRACE_NAME + ".", header, newContext, true);
        newRequestProps.put(RemotingConstants.RPC_TRACE_NAME, newContext);

        newContext = new HashMap();
        ContextMapConverter.treeCopyTo(RemotingConstants.RPC_REQUEST_BAGGAGE + ".", header, newContext,
            true);
        newRequestProps.put(RemotingConstants.RPC_REQUEST_BAGGAGE, newContext);

        newContext = new HashMap();
        ContextMapConverter.treeCopyTo(RemotingConstants.RPC_RESPONSE_BAGGAGE + ".", header,
            newContext, true);
        newRequestProps.put(RemotingConstants.RPC_RESPONSE_BAGGAGE, newContext);

        for (Map.Entry<String, Object> entry : newRequestProps.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        newRequestProps.putAll(header);

        Assert.assertTrue(newRequestProps.size() == 5);
    }

    @Test
    public void treeCopyTo() throws Exception {

    }

}