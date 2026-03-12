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
package com.alipay.sofa.rpc.lookout;

import com.alipay.sofa.rpc.event.rest.RestServerSendEvent;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for RestLookoutAdapter
 *
 * @author SOFA-RPC Team
 */
public class RestLookoutAdapterTest {

    @Test
    public void testSendRestServerSendEventWithNullEventBus() {
        // When EventBus is disabled, the method should not throw exception
        NettyHttpRequest mockRequest = mock(NettyHttpRequest.class);
        RestServerSendEvent event = new RestServerSendEvent(mockRequest, null, null);

        // This should not throw any exception even if EventBus is disabled
        RestLookoutAdapter.sendRestServerSendEvent(event);
    }

    @Test
    public void testSendRestServerSendEventWithThrowable() {
        // Test with throwable - should not throw exception
        NettyHttpRequest mockRequest = mock(NettyHttpRequest.class);
        RuntimeException testException = new RuntimeException("Test error");
        RestServerSendEvent event = new RestServerSendEvent(mockRequest, null, testException);

        // This should not throw any exception
        RestLookoutAdapter.sendRestServerSendEvent(event);
    }

    @Test
    public void testSendRestServerSendEventWithNullThrowable() {
        // Test with null throwable - should not throw exception
        NettyHttpRequest mockRequest = mock(NettyHttpRequest.class);
        RestServerSendEvent event = new RestServerSendEvent(mockRequest, null, null);

        // This should not throw any exception
        RestLookoutAdapter.sendRestServerSendEvent(event);
    }
}
