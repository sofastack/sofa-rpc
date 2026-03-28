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
package com.alipay.sofa.rpc.event.rest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for RestServerSendEvent
 *
 * @author SOFA-RPC Team
 */
public class RestServerSendEventTest {

    @Test
    public void testConstructorAndGetters() {
        org.jboss.resteasy.plugins.server.netty.NettyHttpRequest mockRequest =
                Mockito.mock(org.jboss.resteasy.plugins.server.netty.NettyHttpRequest.class);
        org.jboss.resteasy.plugins.server.netty.NettyHttpResponse mockResponse =
                Mockito.mock(org.jboss.resteasy.plugins.server.netty.NettyHttpResponse.class);
        Throwable throwable = new RuntimeException("test error");

        RestServerSendEvent event = new RestServerSendEvent(mockRequest, mockResponse, throwable);

        Assert.assertNotNull(event);
        Assert.assertEquals(mockRequest, event.getRequest());
        Assert.assertEquals(mockResponse, event.getResponse());
        Assert.assertEquals(throwable, event.getThrowable());
    }

    @Test
    public void testConstructorWithNullThrowable() {
        org.jboss.resteasy.plugins.server.netty.NettyHttpRequest mockRequest =
                Mockito.mock(org.jboss.resteasy.plugins.server.netty.NettyHttpRequest.class);
        org.jboss.resteasy.plugins.server.netty.NettyHttpResponse mockResponse =
                Mockito.mock(org.jboss.resteasy.plugins.server.netty.NettyHttpResponse.class);

        RestServerSendEvent event = new RestServerSendEvent(mockRequest, mockResponse, null);

        Assert.assertNotNull(event);
        Assert.assertNull(event.getThrowable());
    }
}
