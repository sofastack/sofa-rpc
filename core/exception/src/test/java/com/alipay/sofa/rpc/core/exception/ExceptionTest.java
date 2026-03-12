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
package com.alipay.sofa.rpc.core.exception;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for SOFA RPC Exception classes
 *
 * @author SOFA-RPC Team
 */
public class ExceptionTest {

    @Test
    public void testRpcErrorTypeConstants() {
        // Test UNKNOWN error type
        Assert.assertEquals(0, RpcErrorType.UNKNOWN);

        // Test server error types (100-199)
        Assert.assertEquals(100, RpcErrorType.SERVER_BUSY);
        Assert.assertEquals(101, RpcErrorType.SERVER_CLOSED);
        Assert.assertEquals(110, RpcErrorType.SERVER_NOT_FOUND_INVOKER);
        Assert.assertEquals(120, RpcErrorType.SERVER_SERIALIZE);
        Assert.assertEquals(130, RpcErrorType.SERVER_DESERIALIZE);
        Assert.assertEquals(150, RpcErrorType.SERVER_NETWORK);
        Assert.assertEquals(160, RpcErrorType.SERVER_BIZ);
        Assert.assertEquals(170, RpcErrorType.SERVER_FILTER);
        Assert.assertEquals(199, RpcErrorType.SERVER_UNDECLARED_ERROR);

        // Test client error types (200-299)
        Assert.assertEquals(200, RpcErrorType.CLIENT_TIMEOUT);
        Assert.assertEquals(210, RpcErrorType.CLIENT_ROUTER);
        Assert.assertEquals(220, RpcErrorType.CLIENT_SERIALIZE);
        Assert.assertEquals(230, RpcErrorType.CLIENT_DESERIALIZE);
        Assert.assertEquals(250, RpcErrorType.CLIENT_NETWORK);
        Assert.assertEquals(260, RpcErrorType.CLIENT_CALL_TYPE);
        Assert.assertEquals(270, RpcErrorType.CLIENT_FILTER);
        Assert.assertEquals(299, RpcErrorType.CLIENT_UNDECLARED_ERROR);
    }

    @Test
    public void testSofaRpcException_WithErrorTypeAndMessage() {
        SofaRpcException exception = new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT, "Connection timeout");

        Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, exception.getErrorType());
        Assert.assertEquals("Connection timeout", exception.getMessage());
        Assert.assertNull(exception.getCause());
    }

    @Test
    public void testSofaRpcException_WithErrorTypeAndCause() {
        Throwable cause = new RuntimeException("Underlying cause");
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, cause);

        Assert.assertEquals(RpcErrorType.SERVER_BUSY, exception.getErrorType());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Underlying cause", exception.getCause().getMessage());
    }

    @Test
    public void testSofaRpcException_WithErrorTypeMessageAndCause() {
        Throwable cause = new RuntimeException("Underlying cause");
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_NETWORK, "Network error", cause);

        Assert.assertEquals(RpcErrorType.SERVER_NETWORK, exception.getErrorType());
        Assert.assertEquals("Network error", exception.getMessage());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Underlying cause", exception.getCause().getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSofaRpcException_DeprecatedConstructorWithMessage() {
        SofaRpcException exception = new SofaRpcException("Deprecated constructor test");

        Assert.assertEquals(RpcErrorType.UNKNOWN, exception.getErrorType());
        Assert.assertEquals("Deprecated constructor test", exception.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSofaRpcException_DeprecatedConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("Cause message");
        SofaRpcException exception = new SofaRpcException("Deprecated constructor test", cause);

        Assert.assertEquals(RpcErrorType.UNKNOWN, exception.getErrorType());
        Assert.assertEquals("Deprecated constructor test", exception.getMessage());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Cause message", exception.getCause().getMessage());
    }

    @Test
    public void testSofaRpcException_DefaultConstructor() {
        SofaRpcException exception = new SofaRpcException() {
        };

        Assert.assertEquals(RpcErrorType.UNKNOWN, exception.getErrorType());
        Assert.assertNull(exception.getMessage());
    }

    @Test
    public void testSofaTimeOutException_WithMessage() {
        SofaTimeOutException exception = new SofaTimeOutException("Request timeout");

        Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, exception.getErrorType());
        Assert.assertEquals("Request timeout", exception.getMessage());
        Assert.assertNull(exception.getCause());
    }

    @Test
    public void testSofaTimeOutException_WithCause() {
        Throwable cause = new RuntimeException("Connection reset");
        SofaTimeOutException exception = new SofaTimeOutException(cause);

        Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, exception.getErrorType());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Connection reset", exception.getCause().getMessage());
    }

    @Test
    public void testSofaTimeOutException_WithMessageAndCause() {
        Throwable cause = new RuntimeException("IO error");
        SofaTimeOutException exception = new SofaTimeOutException("Timeout occurred", cause);

        Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, exception.getErrorType());
        Assert.assertEquals("Timeout occurred", exception.getMessage());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("IO error", exception.getCause().getMessage());
    }

    @Test
    public void testSofaRouteException_WithMessage() {
        SofaRouteException exception = new SofaRouteException("No provider found");

        Assert.assertEquals(RpcErrorType.CLIENT_ROUTER, exception.getErrorType());
        Assert.assertEquals("No provider found", exception.getMessage());
        Assert.assertNull(exception.getCause());
    }

    @Test
    public void testSofaRouteException_WithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("Invalid router config");
        SofaRouteException exception = new SofaRouteException("Router error", cause);

        Assert.assertEquals(RpcErrorType.CLIENT_ROUTER, exception.getErrorType());
        Assert.assertEquals("Router error", exception.getMessage());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Invalid router config", exception.getCause().getMessage());
    }

    @Test
    public void testSofaRpcRuntimeException_WithMessage() {
        SofaRpcRuntimeException exception = new SofaRpcRuntimeException("Runtime error occurred");

        Assert.assertEquals("Runtime error occurred", exception.getMessage());
        Assert.assertNull(exception.getCause());
    }

    @Test
    public void testSofaRpcRuntimeException_WithMessageAndCause() {
        Throwable cause = new NullPointerException("Null pointer");
        SofaRpcRuntimeException exception = new SofaRpcRuntimeException("Runtime error", cause);

        Assert.assertEquals("Runtime error", exception.getMessage());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Null pointer", exception.getCause().getMessage());
    }

    @Test
    public void testSofaRpcRuntimeException_WithCause() {
        Throwable cause = new IllegalStateException("Illegal state");
        SofaRpcRuntimeException exception = new SofaRpcRuntimeException(cause);

        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals("Illegal state", exception.getCause().getMessage());
    }

    @Test
    public void testExceptionInheritance() {
        // Verify inheritance hierarchy
        Assert.assertTrue(new SofaRpcException() {
        } instanceof RuntimeException);
        Assert.assertTrue(new SofaTimeOutException("test") instanceof SofaRpcException);
        Assert.assertTrue(new SofaRouteException("test") instanceof SofaRpcException);
        Assert.assertTrue(new SofaRpcRuntimeException("test") instanceof RuntimeException);
    }

    @Test
    public void testExceptionMessagePropagation() {
        // Test that messages are properly propagated through exception hierarchy
        String message = "Test exception message";

        SofaRpcException baseException = new SofaRpcException(RpcErrorType.UNKNOWN, message);
        Assert.assertEquals(message, baseException.getMessage());

        SofaTimeOutException timeoutException = new SofaTimeOutException(message);
        Assert.assertEquals(message, timeoutException.getMessage());

        SofaRouteException routeException = new SofaRouteException(message);
        Assert.assertEquals(message, routeException.getMessage());

        SofaRpcRuntimeException runtimeException = new SofaRpcRuntimeException(message);
        Assert.assertEquals(message, runtimeException.getMessage());
    }
}
