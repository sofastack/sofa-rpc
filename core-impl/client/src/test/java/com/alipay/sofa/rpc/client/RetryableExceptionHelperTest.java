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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class RetryableExceptionHelperTest {

    @Test
    public void testShouldRetryDefaultSofaRpcException() {
        Assert.assertTrue(RetryableExceptionHelper.shouldRetry(new SofaRpcException(RpcErrorType.SERVER_BUSY, "busy"),
            RetryableExceptionHelper.resolveRetryExceptions(null)));
        Assert.assertTrue(RetryableExceptionHelper.shouldRetry(new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT,
            "timeout"), RetryableExceptionHelper.resolveRetryExceptions(null)));
        Assert.assertFalse(RetryableExceptionHelper.shouldRetry(new SofaRpcException(
            RpcErrorType.CLIENT_UNDECLARED_ERROR, "fatal"), RetryableExceptionHelper.resolveRetryExceptions(null)));
    }

    @Test
    public void testResolveRetryExceptionsAndMatchCause() {
        List<Class<? extends Throwable>> retryableExceptions = RetryableExceptionHelper
            .resolveRetryExceptions("java.lang.RuntimeException;java.lang.IllegalStateException");

        Assert.assertEquals(2, retryableExceptions.size());
        Assert.assertTrue(RetryableExceptionHelper.shouldRetry(new IllegalArgumentException("bad argument"),
            retryableExceptions));
        Assert.assertTrue(RetryableExceptionHelper.shouldRetry(new Exception(new IllegalStateException("bad state")),
            retryableExceptions));
        Assert.assertFalse(RetryableExceptionHelper.shouldRetry(new IOException("io"), retryableExceptions));
    }

    @Test
    public void testGetResponseThrowableAndBuildFailureMessage() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse(new IllegalStateException("retry"));

        Assert.assertTrue(RetryableExceptionHelper.getResponseThrowable(response) instanceof IllegalStateException);
        Assert.assertEquals("class java.lang.IllegalStateException:retry",
            RetryableExceptionHelper.buildFailureMessage(null, response));
    }

    @Test
    public void testShouldRetryRemoteExceptionClass() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse(new SofaRpcException(RpcErrorType.SERVER_BIZ, "remote error"));
        response.addResponseProp(RemotingConstants.HEAD_RESPONSE_EXCEPTION, IllegalStateException.class.getName());

        List<Class<? extends Throwable>> retryableExceptions = RetryableExceptionHelper
            .resolveRetryExceptions(RuntimeException.class.getName());

        Assert.assertTrue(RetryableExceptionHelper.shouldRetry(response, retryableExceptions));
    }

    @Test
    public void testResolveRetryExceptionsRejectsInvalidClass() {
        try {
            RetryableExceptionHelper.resolveRetryExceptions("java.lang.String");
            Assert.fail("should reject non throwable retry exception");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Retry exception class must extend Throwable"));
        }

        try {
            RetryableExceptionHelper.resolveRetryExceptions("not.exists.RetryException");
            Assert.fail("should reject invalid retry exception class");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to load retry exception class"));
        }
    }
}
