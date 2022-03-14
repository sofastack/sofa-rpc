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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.core.util.SafeConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class BaggageResolverTest {

    public static final String KEY   = "key";
    public static final String VALUE = "value";
    private static boolean     oldEnableBaggage;
    private static Field       enableBaggageField;

    static {
        try {
            enableBaggageField = RpcInvokeContext.class.getDeclaredField("BAGGAGE_ENABLE");
            enableBaggageField.setAccessible(true);
            oldEnableBaggage = (boolean) enableBaggageField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void before() {
        try {
            enableBaggageField.set(null, true);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @After
    public void after() {
        try {
            RpcInvokeContext.removeContext();
            enableBaggageField.set(null, oldEnableBaggage);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCarryWithRequest() {
        SofaRequest sofaRequest = new SofaRequest();
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.putRequestBaggage(KEY, VALUE);
        BaggageResolver.carryWithRequest(context, sofaRequest);
        Object baggage = sofaRequest.getRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE);
        assertNotNull(baggage);
        assertEquals(HashMap.class, baggage.getClass());
        assertEquals(VALUE, ((Map) baggage).get(KEY));
    }

    @Test
    public void testPickupFromRequest() {
        SofaRequest sofaRequest = new SofaRequest();
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.putRequestBaggage(KEY, VALUE);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(null, "null");
        hashMap.put(KEY, VALUE);
        sofaRequest.addRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE, hashMap);
        BaggageResolver.pickupFromRequest(context, sofaRequest);
        assertEquals(VALUE, RpcInvokeContext.getContext().getAllRequestBaggage().get(KEY));
        assertEquals(null, RpcInvokeContext.getContext().getAllRequestBaggage().get(null));
        assertEquals(1, RpcInvokeContext.getContext().getAllRequestBaggage().size());
        assertEquals(SafeConcurrentHashMap.class, RpcInvokeContext.getContext().getAllRequestBaggage().getClass());
    }

    @Test
    public void testCarryWithResponse() {
        SofaResponse sofaResponse = new SofaResponse();
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.putResponseBaggage(KEY, VALUE);
        BaggageResolver.carryWithResponse(context, sofaResponse);
        String baggage = (String) sofaResponse.getResponseProp(RemotingConstants.RPC_RESPONSE_BAGGAGE
            + "." + KEY);
        assertNotNull(baggage);
        assertEquals(VALUE, baggage);
    }

    @Test
    public void testPickupFromResponse() {
        SofaResponse sofaResponse = new SofaResponse();
        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.putResponseBaggage(KEY, VALUE);
        sofaResponse.addResponseProp(RemotingConstants.RPC_RESPONSE_BAGGAGE + ".key", VALUE);
        sofaResponse.addResponseProp(RemotingConstants.RPC_RESPONSE_BAGGAGE + ".key2", null);
        BaggageResolver.pickupFromResponse(context, sofaResponse);
        assertEquals(VALUE, RpcInvokeContext.getContext().getAllResponseBaggage().get(KEY));
        assertEquals(null, RpcInvokeContext.getContext().getAllResponseBaggage().get("key2"));
        assertEquals(1, RpcInvokeContext.getContext().getAllResponseBaggage().size());
        assertEquals(SafeConcurrentHashMap.class, RpcInvokeContext.getContext()
            .getAllResponseBaggage().getClass());
    }

}