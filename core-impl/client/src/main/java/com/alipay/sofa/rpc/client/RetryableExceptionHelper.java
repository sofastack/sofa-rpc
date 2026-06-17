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
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RetryableExceptionHelper {

    private RetryableExceptionHelper() {
    }

    static boolean shouldRetry(SofaRpcException exception, List<Class<? extends Throwable>> retryableExceptions) {
        return isDefaultRetryable(exception) || matches(exception, retryableExceptions);
    }

    static boolean shouldRetry(Throwable throwable, List<Class<? extends Throwable>> retryableExceptions) {
        return matches(throwable, retryableExceptions);
    }

    static boolean shouldRetry(SofaResponse response, List<Class<? extends Throwable>> retryableExceptions) {
        return matches(getResponseThrowable(response), retryableExceptions)
            || matchesRemoteException(response, retryableExceptions);
    }

    static Throwable getResponseThrowable(SofaResponse response) {
        if (response == null) {
            return null;
        }
        Object appResponse = response.getAppResponse();
        return appResponse instanceof Throwable ? (Throwable) appResponse : null;
    }

    static List<Class<? extends Throwable>> resolveRetryExceptions(String retryExceptions) {
        if (StringUtils.isBlank(retryExceptions)) {
            return Collections.emptyList();
        }
        String[] classNames = StringUtils.splitWithCommaOrSemicolon(retryExceptions);
        List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>(classNames.length);
        for (String className : classNames) {
            Class<?> exceptionClass;
            try {
                exceptionClass = ClassUtils.forName(className);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Failed to load retry exception class: " + className, e);
            }
            if (!Throwable.class.isAssignableFrom(exceptionClass)) {
                throw new IllegalArgumentException("Retry exception class must extend Throwable: " + className);
            }
            result.add(castThrowableClass(exceptionClass));
        }
        return result;
    }

    static String buildFailureMessage(SofaRpcException exception, SofaResponse response) {
        Throwable failure = exception != null ? exception : getResponseThrowable(response);
        if (failure == null) {
            return null;
        }
        return failure.getClass() + ":" + failure.getMessage();
    }

    private static boolean isDefaultRetryable(SofaRpcException exception) {
        return exception != null && (exception.getErrorType() == RpcErrorType.SERVER_BUSY
            || exception.getErrorType() == RpcErrorType.CLIENT_TIMEOUT);
    }

    private static boolean matches(Throwable throwable, List<Class<? extends Throwable>> retryableExceptions) {
        if (throwable == null || retryableExceptions.isEmpty()) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            for (Class<? extends Throwable> retryableException : retryableExceptions) {
                if (retryableException.isAssignableFrom(current.getClass())) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean matchesRemoteException(SofaResponse response,
                                                  List<Class<? extends Throwable>> retryableExceptions) {
        if (response == null || retryableExceptions.isEmpty()) {
            return false;
        }
        Object exceptionClassName = response.getResponseProp(RemotingConstants.HEAD_RESPONSE_EXCEPTION);
        if (!(exceptionClassName instanceof String) || StringUtils.isBlank((String) exceptionClassName)) {
            return false;
        }
        Class<?> exceptionClass;
        try {
            exceptionClass = ClassUtils.forName((String) exceptionClassName);
        } catch (RuntimeException e) {
            return matchesClassName((String) exceptionClassName, retryableExceptions);
        }
        if (!Throwable.class.isAssignableFrom(exceptionClass)) {
            return false;
        }
        for (Class<? extends Throwable> retryableException : retryableExceptions) {
            if (retryableException.isAssignableFrom(exceptionClass)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesClassName(String exceptionClassName,
                                            List<Class<? extends Throwable>> retryableExceptions) {
        for (Class<? extends Throwable> retryableException : retryableExceptions) {
            if (retryableException.getName().equals(exceptionClassName)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Throwable> castThrowableClass(Class<?> exceptionClass) {
        return (Class<? extends Throwable>) exceptionClass;
    }
}
