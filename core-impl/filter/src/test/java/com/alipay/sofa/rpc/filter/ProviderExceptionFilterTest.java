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

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for ProviderExceptionFilter
 *
 * @author SOFA-RPC Team
 */
public class ProviderExceptionFilterTest {

    @Test
    public void testInvokeSuccess() throws SofaRpcException {
        ProviderExceptionFilter filter = new ProviderExceptionFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse expectedResponse = new SofaResponse();

        Mockito.when(invoker.invoke(request)).thenReturn(expectedResponse);

        SofaResponse response = filter.invoke(invoker, request);

        Assert.assertSame(expectedResponse, response);
        Mockito.verify(invoker).invoke(request);
    }

    @Test
    public void testInvokeWithSofaRpcException() {
        ProviderExceptionFilter filter = new ProviderExceptionFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaRpcException expectedException = new SofaRpcException(RpcErrorType.SERVER_BUSY, "busy");

        Mockito.when(invoker.invoke(request)).thenThrow(expectedException);

        try {
            filter.invoke(invoker, request);
            Assert.fail("Expected SofaRpcException to be thrown");
        } catch (SofaRpcException e) {
            Assert.assertSame(expectedException, e);
        }
    }

    @Test
    public void testInvokeWithThrowable() {
        ProviderExceptionFilter filter = new ProviderExceptionFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        RuntimeException cause = new RuntimeException("Test exception");

        Mockito.when(invoker.invoke(request)).thenThrow(cause);

        try {
            filter.invoke(invoker, request);
            Assert.fail("Expected SofaRpcException to be thrown");
        } catch (SofaRpcException e) {
            Assert.assertEquals(RpcErrorType.SERVER_FILTER, e.getErrorType());
            Assert.assertSame(cause, e.getCause());
        }
    }

    @Test
    public void testInvokeWithError() {
        ProviderExceptionFilter filter = new ProviderExceptionFilter();
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        Error error = new StackOverflowError("test");

        Mockito.when(invoker.invoke(request)).thenThrow(error);

        try {
            filter.invoke(invoker, request);
            Assert.fail("Expected SofaRpcException to be thrown");
        } catch (SofaRpcException e) {
            Assert.assertEquals(RpcErrorType.SERVER_FILTER, e.getErrorType());
            Assert.assertSame(error, e.getCause());
        }
    }

    @Test
    public void testExtensionAnnotation() {
        ProviderExceptionFilter filter = new ProviderExceptionFilter();
        com.alipay.sofa.rpc.ext.Extension extension =
                filter.getClass().getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);

        Assert.assertNotNull(extension);
        Assert.assertEquals("providerException", extension.value());
        Assert.assertEquals(-20000, extension.order());
    }
}
