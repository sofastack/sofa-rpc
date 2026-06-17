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

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class FailoverClusterTest {

    @Test
    public void testRetryCustomResponseException() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(CustomRetryException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            response(new CustomRetryException("retry once")), response("success"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertEquals("success", response.getAppResponse());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testRetryRemoteResponseExceptionClass() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(CustomRetryException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            remoteExceptionResponse(CustomRetryException.class), response("success"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertEquals("success", response.getAppResponse());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testReturnLastResponseWhenCustomResponseExceptionExhausted() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(CustomRetryException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            response(new CustomRetryException("retry-1")), response(new CustomRetryException("retry-2")));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertTrue(response.getAppResponse() instanceof CustomRetryException);
        Assert.assertEquals("retry-2", ((CustomRetryException) response.getAppResponse()).getMessage());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testRetryConfiguredThrownException() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(IllegalStateException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, new IllegalStateException("retry me"),
            response("success"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertEquals("success", response.getAppResponse());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testBuiltinRetryableSofaRpcExceptionStillWorks() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT, "timeout"), response("success"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertEquals("success", response.getAppResponse());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testMethodRetryExceptionOverride() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(IllegalStateException.class.getName());
        MethodConfig methodConfig = new MethodConfig().setName("sayHello")
            .setRetryExceptions(IllegalArgumentException.class.getName());
        consumerConfig.setMethods(Arrays.asList(methodConfig));
        consumerConfig.getConfigValueCache(true);

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, new IllegalArgumentException("retry"),
            response("success"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertEquals("success", response.getAppResponse());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testThrowOriginalRetryCauseWhenLaterFailureIsNotRetryable() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT, "timeout"),
            new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "fatal"));

        try {
            cluster.doInvoke(createRequest());
            Assert.fail("should throw the retry cause");
        } catch (SofaRpcException e) {
            Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, e.getErrorType());
        }
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testReturnRetryableResponseWhenLaterRpcExceptionIsNotRetryable() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(CustomRetryException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            response(new CustomRetryException("retry-response")),
            new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "fatal"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertTrue(response.getAppResponse() instanceof CustomRetryException);
        Assert.assertEquals("retry-response", ((CustomRetryException) response.getAppResponse()).getMessage());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testThrowRetryableWrappedExceptionWhenLaterExceptionIsNotRetryable() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(IllegalStateException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, new IllegalStateException("retry"),
            new IllegalArgumentException("fatal"));

        try {
            cluster.doInvoke(createRequest());
            Assert.fail("should throw wrapped retryable exception");
        } catch (SofaRpcException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalStateException);
            Assert.assertEquals("retry", e.getCause().getMessage());
        }
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testReturnRetryableResponseWhenLaterExceptionIsNotRetryable() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(CustomRetryException.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig,
            response(new CustomRetryException("retry-response")), new IllegalArgumentException("fatal"));

        SofaResponse response = cluster.doInvoke(createRequest());

        Assert.assertTrue(response.getAppResponse() instanceof CustomRetryException);
        Assert.assertEquals("retry-response", ((CustomRetryException) response.getAppResponse()).getMessage());
        Assert.assertEquals(2, cluster.getInvokeTimes());
    }

    @Test
    public void testThrowWrappedExceptionWhenExceptionIsNotRetryable() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(0);

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, new IllegalArgumentException("fatal"));

        try {
            cluster.doInvoke(createRequest());
            Assert.fail("should throw wrapped exception");
        } catch (SofaRpcException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals("fatal", e.getCause().getMessage());
        }
        Assert.assertEquals(1, cluster.getInvokeTimes());
    }

    @Test
    public void testThrowUndeclaredExceptionWhenResponseIsNull() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(0);

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, (Object) null);

        try {
            cluster.doInvoke(createRequest());
            Assert.fail("should throw undeclared exception");
        } catch (SofaRpcException e) {
            Assert.assertEquals(RpcErrorType.CLIENT_UNDECLARED_ERROR, e.getErrorType());
            Assert.assertTrue(e.getMessage().contains("return null"));
        }
        Assert.assertEquals(1, cluster.getInvokeTimes());
    }

    @Test
    public void testInvalidRetryExceptionConfigFailsFast() {
        ConsumerConfig<Object> consumerConfig = createConsumerConfig();
        consumerConfig.setRetries(1);
        consumerConfig.setRetryExceptions(String.class.getName());

        TestFailoverCluster cluster = new TestFailoverCluster(consumerConfig, response("ignored"));

        try {
            cluster.doInvoke(createRequest());
            Assert.fail("should reject invalid retry exception configuration");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Retry exception class must extend Throwable"));
        }
        Assert.assertEquals(0, cluster.getInvokeTimes());
    }

    private static ConsumerConfig<Object> createConsumerConfig() {
        return new ConsumerConfig<Object>().setProtocol("test")
            .setBootstrap("test")
            .setApplication(new ApplicationConfig().setAppName("retry-test"))
            .setInterfaceId(TestService.class.getName());
    }

    private static SofaRequest createRequest() {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(TestService.class.getName());
        request.setMethodName("sayHello");
        return request;
    }

    private static SofaResponse response(Object appResponse) {
        SofaResponse response = new SofaResponse();
        response.setAppResponse(appResponse);
        return response;
    }

    private static SofaResponse remoteExceptionResponse(Class<? extends Throwable> exceptionClass) {
        SofaResponse response = response(new SofaRpcException(RpcErrorType.SERVER_BIZ, "remote error"));
        response.addResponseProp(RemotingConstants.HEAD_RESPONSE_EXCEPTION, exceptionClass.getName());
        return response;
    }

    public interface TestService {
        String sayHello();
    }

    public static class CustomRetryException extends RuntimeException {
        public CustomRetryException(String message) {
            super(message);
        }
    }

    private static class TestFailoverCluster extends FailoverCluster {
        private final Deque<Object> results;
        private int                 invokeTimes;

        TestFailoverCluster(ConsumerConfig<Object> consumerConfig, Object... results) {
            super(new ConsumerBootstrap<Object>(consumerConfig) {
                @Override
                public Object refer() {
                    return null;
                }

                @Override
                public void unRefer() {
                }

                @Override
                public Object getProxyIns() {
                    return null;
                }

                @Override
                public Cluster getCluster() {
                    return null;
                }

                @Override
                public List<ProviderGroup> subscribe() {
                    return null;
                }

                @Override
                public boolean isSubscribed() {
                    return false;
                }
            });
            this.results = new LinkedList<Object>(Arrays.asList(results));
        }

        @Override
        protected ProviderInfo select(SofaRequest message, List<ProviderInfo> invokedProviderInfos) {
            ProviderInfo providerInfo = new ProviderInfo();
            providerInfo.setHost("127.0.0.1");
            providerInfo.setPort(12200 + invokeTimes);
            return providerInfo;
        }

        @Override
        protected SofaResponse filterChain(ProviderInfo providerInfo, SofaRequest request) {
            invokeTimes++;
            Object result = results.removeFirst();
            if (result instanceof SofaRpcException) {
                throw (SofaRpcException) result;
            } else if (result instanceof RuntimeException) {
                throw (RuntimeException) result;
            }
            return (SofaResponse) result;
        }

        int getInvokeTimes() {
            return invokeTimes;
        }
    }
}
