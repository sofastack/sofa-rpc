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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for FallbackContext
 *
 * @author SOFA-RPC Team
 */
public class FallbackContextTest {

    @Test
    public void testDefaultConstructor() {
        FallbackContext context = new FallbackContext();
        Assert.assertNotNull(context);
        Assert.assertNull(context.getInvoker());
        Assert.assertNull(context.getRequest());
        Assert.assertNull(context.getResponse());
        Assert.assertNull(context.getException());
    }

    @Test
    public void testConstructorWithParameters() {
        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse response = new SofaResponse();
        Throwable exception = new RuntimeException("test");

        FallbackContext context = new FallbackContext(invoker, request, response, exception);

        Assert.assertNotNull(context);
        Assert.assertEquals(invoker, context.getInvoker());
        Assert.assertEquals(request, context.getRequest());
        Assert.assertEquals(response, context.getResponse());
        Assert.assertEquals(exception, context.getException());
    }

    @Test
    public void testSetters() {
        FallbackContext context = new FallbackContext();

        FilterInvoker invoker = Mockito.mock(FilterInvoker.class);
        SofaRequest request = new SofaRequest();
        SofaResponse response = new SofaResponse();
        Throwable exception = new RuntimeException("test");

        context.setInvoker(invoker);
        context.setRequest(request);
        context.setResponse(response);
        context.setException(exception);

        Assert.assertEquals(invoker, context.getInvoker());
        Assert.assertEquals(request, context.getRequest());
        Assert.assertEquals(response, context.getResponse());
        Assert.assertEquals(exception, context.getException());
    }
}
