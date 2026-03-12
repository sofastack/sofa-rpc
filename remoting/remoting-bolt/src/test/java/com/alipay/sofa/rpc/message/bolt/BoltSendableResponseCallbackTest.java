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
package com.alipay.sofa.rpc.message.bolt;

import com.alipay.remoting.AsyncContext;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit test for BoltSendableResponseCallback
 *
 * @author <a href="mailto:xxx@antfin.com">XXX</a>
 */
public class BoltSendableResponseCallbackTest {

    private AsyncContext mockAsyncContext;
    private SofaRequest  mockRequest;

    @Before
    public void setUp() {
        mockAsyncContext = Mockito.mock(AsyncContext.class);
        mockRequest = Mockito.mock(SofaRequest.class);

        RpcInternalContext context = RpcInternalContext.getContext();
        context.setAttachment(RpcConstants.HIDDEN_KEY_ASYNC_CONTEXT, mockAsyncContext);
        context.setAttachment(RpcConstants.HIDDEN_KEY_ASYNC_REQUEST, mockRequest);
    }

    @After
    public void tearDown() {
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Test
    public void testConstructor() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        Assert.assertNotNull(callback);
    }

    @Test
    public void testSendAppResponse() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        Object appResponse = "testResponse";
        callback.sendAppResponse(appResponse);

        ArgumentCaptor<SofaResponse> captor = ArgumentCaptor.forClass(SofaResponse.class);
        Mockito.verify(mockAsyncContext, Mockito.times(1)).sendResponse(captor.capture());

        SofaResponse capturedResponse = captor.getValue();
        Assert.assertNotNull(capturedResponse);
        Assert.assertEquals(appResponse, capturedResponse.getAppResponse());
    }

    @Test
    public void testSendAppException() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        Throwable throwable = new RuntimeException("Business Error");
        callback.sendAppException(throwable);

        ArgumentCaptor<SofaResponse> captor = ArgumentCaptor.forClass(SofaResponse.class);
        Mockito.verify(mockAsyncContext, Mockito.times(1)).sendResponse(captor.capture());

        SofaResponse capturedResponse = captor.getValue();
        Assert.assertNotNull(capturedResponse);
        Assert.assertEquals(throwable, capturedResponse.getAppResponse());
    }

    @Test
    public void testSendSofaException() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        SofaRpcException sofaException = new SofaRpcException("RPC Error");
        callback.sendSofaException(sofaException);

        ArgumentCaptor<SofaResponse> captor = ArgumentCaptor.forClass(SofaResponse.class);
        Mockito.verify(mockAsyncContext, Mockito.times(1)).sendResponse(captor.capture());

        SofaResponse capturedResponse = captor.getValue();
        Assert.assertNotNull(capturedResponse);
        Assert.assertEquals(sofaException.getMessage(), capturedResponse.getErrorMsg());
    }

    @Test
    public void testOnAppException() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        Throwable throwable = new RuntimeException("Error");
        callback.onAppException(throwable, "testMethod", mockRequest);

        ArgumentCaptor<SofaResponse> captor = ArgumentCaptor.forClass(SofaResponse.class);
        Mockito.verify(mockAsyncContext, Mockito.times(1)).sendResponse(captor.capture());

        SofaResponse capturedResponse = captor.getValue();
        Assert.assertEquals(throwable, capturedResponse.getAppResponse());
    }

    @Test
    public void testOnSofaException() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        SofaRpcException sofaException = new SofaRpcException("RPC Error");
        callback.onSofaException(sofaException, "testMethod", mockRequest);

        ArgumentCaptor<SofaResponse> captor = ArgumentCaptor.forClass(SofaResponse.class);
        Mockito.verify(mockAsyncContext, Mockito.times(1)).sendResponse(captor.capture());

        SofaResponse capturedResponse = captor.getValue();
        Assert.assertEquals(sofaException.getMessage(), capturedResponse.getErrorMsg());
    }

    @Test
    public void testCannotSendMultipleTimes() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        // First send should succeed
        callback.sendAppResponse("response1");

        // Second send should fail
        try {
            callback.sendAppResponse("response2");
            Assert.fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("has been sent"));
        }
    }

    @Test
    public void testCannotSendExceptionAfterResponse() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        // First send response
        callback.sendAppResponse("response1");

        // Then send exception should fail
        try {
            callback.sendAppException(new RuntimeException("error"));
            Assert.fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("has been sent"));
        }
    }

    @Test
    public void testCannotSendResponseAfterException() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        // First send exception
        callback.sendAppException(new RuntimeException("error"));

        // Then send response should fail
        try {
            callback.sendAppResponse("response");
            Assert.fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("has been sent"));
        }
    }

    @Test
    public void testInitWithNullContext() {
        RpcInternalContext.removeContext();

        // Should not throw exception
        TestSendableResponseCallback callback = new TestSendableResponseCallback();
        Assert.assertNotNull(callback);
    }

    @Test
    public void testCheckState() {
        TestSendableResponseCallback callback = new TestSendableResponseCallback();

        // First checkState should pass and mark as sent
        callback.checkState();

        // Second checkState should fail
        try {
            callback.checkState();
            Assert.fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("has been sent"));
        }
    }

    /**
     * Test subclass for testing protected methods
     */
    static class TestSendableResponseCallback extends BoltSendableResponseCallback<Object> {

        @Override
        public void onAppResponse(Object appResponse, String methodName,
                                  com.alipay.sofa.rpc.core.request.RequestBase request) {
            sendAppResponse(appResponse);
        }

        public void checkState() {
            super.checkState();
        }

        public void sendAppResponse(Object appResponse) {
            super.sendAppResponse(appResponse);
        }

        public void sendAppException(Throwable throwable) {
            super.sendAppException(throwable);
        }

        public void sendSofaException(SofaRpcException sofaException) {
            super.sendSofaException(sofaException);
        }
    }
}
