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
package com.alipay.sofa.rpc.core.request;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaRequestTest {
    @Test
    public void testToString() {
        SofaRequest request = new SofaRequest();
        Assert.assertNotNull(request.toString());
    }

    @Test
    public void getRequestProp() throws Exception {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(Invoker.class.getName());
        request.setMethodName("invoke");
        request.setMethod(Invoker.class.getMethod("invoke", SofaRequest.class));
        request.setMethodArgs(new Object[] { new SofaRequest() });
        request.setMethodArgSigs(new String[] { SofaRequest.class.getCanonicalName() });
        request.setTargetServiceUniqueName(Invoker.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 11);
        request.setTimeout(1024);
        request.setSerializeType((byte) 11);
        request.setData(new ByteArrayWrapperByteBuf(new byte[] { 1, 2, 3 }));
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        request.setSofaResponseCallback(new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {

            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {

            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {

            }
        });

        Assert.assertEquals(Invoker.class.getName(), request.getInterfaceName());
        Assert.assertEquals("invoke", request.getMethodName());
        Assert.assertEquals("invoke", request.getMethod().getName());
        Assert.assertArrayEquals(new String[] { SofaRequest.class.getCanonicalName() },
            request.getMethodArgSigs());
        Assert.assertEquals(1, request.getMethodArgs().length);
        Assert.assertEquals(Invoker.class.getName() + ":1.0", request.getTargetServiceUniqueName());
        Assert.assertEquals("targetApp", request.getTargetAppName());
        Assert.assertTrue(request.getSerializeType() == 11);
        Assert.assertTrue(request.getTimeout() == 1024);
        Assert.assertEquals(RpcConstants.INVOKER_TYPE_SYNC, request.getInvokeType());
        Assert.assertTrue(request.getSerializeType() == 11);
        Assert.assertTrue(request.getData().array().length == 3);
        Assert.assertNotNull(request.getSofaResponseCallback());

        Map<String, Object> map = request.getRequestProps();
        Assert.assertTrue(map == null);

        request.addRequestProp("1", "1");
        map = request.getRequestProps();
        Assert.assertTrue(map.size() == 1);
        request.addRequestProp(null, "1");
        Assert.assertTrue(map.size() == 1);
        request.addRequestProp("1", null);
        Assert.assertTrue(map.size() == 1);

        request.addRequestProps(null);
        Assert.assertTrue(map.size() == 1);
        request.addRequestProps(new HashMap<String, Object>());
        Assert.assertTrue(map.size() == 1);

        request.removeRequestProp("1");
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("1", "1");
        request.addRequestProps(hashMap);
        hashMap.put("2", "2");
        request.addRequestProps(hashMap);
        Assert.assertTrue(map.size() == 2);
        Assert.assertEquals("2", request.getRequestProp("2"));

        request.removeRequestProp(null);
        Assert.assertTrue(map.size() == 2);
        request.removeRequestProp("2");
        Assert.assertTrue(map.size() == 1);
        Assert.assertNull(request.getRequestProp("2"));

        Assert.assertFalse(request.isAsync());

        request.setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE);
        Assert.assertTrue(request.isAsync());

        request.setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK);
        Assert.assertTrue(request.isAsync());

        request.setInvokeType(null);
        Assert.assertFalse(request.isAsync());
    }
}