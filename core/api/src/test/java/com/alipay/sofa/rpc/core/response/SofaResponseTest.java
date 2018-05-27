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
package com.alipay.sofa.rpc.core.response;

import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaResponseTest {
    @Test
    public void testToString() {
        SofaResponse response = new SofaResponse();
        Assert.assertNotNull(response.toString());
    }

    @Test
    public void getResponseProp() {
        SofaResponse response = new SofaResponse();
        response.setErrorMsg(null);
        Assert.assertFalse(response.isError());

        response = new SofaResponse();
        response.setErrorMsg("1233");
        Assert.assertTrue(response.isError());
        Assert.assertEquals("1233", response.getErrorMsg());

        response = new SofaResponse();
        response.setAppResponse(new RuntimeException("1233"));
        Assert.assertTrue(response.getAppResponse() instanceof RuntimeException);
        Assert.assertFalse(response.isError());

        response = new SofaResponse();
        response.setAppResponse("1233");
        Assert.assertFalse(response.isError());
        Assert.assertEquals("1233", response.getAppResponse());

        response.setSerializeType((byte) 11);
        response.setData(new ByteArrayWrapperByteBuf(new byte[] { 1, 2, 3 }));
        Assert.assertTrue(response.getSerializeType() == 11);
        Assert.assertTrue(response.getData().array().length == 3);

        Map<String, String> map = response.getResponseProps();
        Assert.assertTrue(map == null);

        response.addResponseProp("1", "1");
        map = response.getResponseProps();
        Assert.assertTrue(map.size() == 1);
        response.addResponseProp(null, "1");
        Assert.assertTrue(map.size() == 1);
        response.addResponseProp("1", null);
        Assert.assertTrue(map.size() == 1);

        response.removeResponseProp(null);
        Assert.assertTrue(map.size() == 1);
        response.removeResponseProp("1");
        Assert.assertTrue(map.size() == 0);
        Assert.assertNull(response.getResponseProp("1"));

        response.setResponseProps(Collections.singletonMap("1", "1"));
        Assert.assertTrue(response.getResponseProps().size() == 1);
    }
}