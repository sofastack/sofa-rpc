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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.common.utils.Ordered;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

/**
 * Unit tests for ConsumerCustomHeaderFilter
 *
 * @author SOFA-RPC Team
 */
public class ConsumerCustomHeaderFilterTest {

    @Test
    public void testInvokeWithCustomHeader() throws SofaRpcException {
        ConsumerCustomHeaderFilter filter = new ConsumerCustomHeaderFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse expectedResponse = new SofaResponse();

        Mockito.when(invoker.invoke(request)).thenReturn(expectedResponse);

        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.addCustomHeader("key1", "value1");
        context.addCustomHeader("key2", "value2");

        try {
            SofaResponse response = filter.invoke(invoker, request);

            Assert.assertSame(expectedResponse, response);
            Mockito.verify(invoker).invoke(request);

            Map<String, Object> requestProps = request.getRequestProps();
            Assert.assertEquals("value1", requestProps.get("key1"));
            Assert.assertEquals("value2", requestProps.get("key2"));
        } finally {
            context.clearCustomHeader();
        }
    }

    @Test
    public void testInvokeWithEmptyCustomHeader() throws SofaRpcException {
        ConsumerCustomHeaderFilter filter = new ConsumerCustomHeaderFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse expectedResponse = new SofaResponse();

        Mockito.when(invoker.invoke(request)).thenReturn(expectedResponse);

        SofaResponse response = filter.invoke(invoker, request);

        Assert.assertSame(expectedResponse, response);
        Mockito.verify(invoker).invoke(request);
    }

    @Test
    public void testCustomHeaderClearedAfterInvoke() throws SofaRpcException {
        ConsumerCustomHeaderFilter filter = new ConsumerCustomHeaderFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse expectedResponse = new SofaResponse();

        Mockito.when(invoker.invoke(request)).thenReturn(expectedResponse);

        RpcInvokeContext context = RpcInvokeContext.getContext();
        context.addCustomHeader("key1", "value1");

        try {
            filter.invoke(invoker, request);

            Assert.assertTrue(context.getCustomHeader().isEmpty());
        } finally {
            context.clearCustomHeader();
        }
    }

    @Test
    public void testExtensionAnnotation() {
        ConsumerCustomHeaderFilter filter = new ConsumerCustomHeaderFilter();
        com.alipay.sofa.rpc.ext.Extension extension =
                filter.getClass().getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);

        Assert.assertNotNull(extension);
        Assert.assertEquals("consumerCustomHeader", extension.value());
        Assert.assertEquals(Ordered.LOWEST_PRECEDENCE, extension.order());
    }
}
