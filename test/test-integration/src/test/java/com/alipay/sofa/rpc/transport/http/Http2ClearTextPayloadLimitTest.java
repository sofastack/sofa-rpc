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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.http.HttpService;
import com.alipay.sofa.rpc.server.http.HttpServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 回归测试：HTTP/2 (h2c) 路径必须对请求体累计长度施加与 HTTP/1.1 一致的 payload 上限，
 * 超限时重置 stream 且不调用业务方法，避免恶意客户端持续发送 DATA 帧导致堆耗尽 (CWE-770)。
 *
 * @author <a href="mailto:liujianjun.ljj@antfin.com">Jianjun Liu</a>
 */
public class Http2ClearTextPayloadLimitTest extends ActivelyDestroyTest {

    /**
     * 业务方法被调用的次数，用于校验超限请求不会进入业务处理。
     */
    private final AtomicInteger invoked = new AtomicInteger();

    @Test
    public void testPayloadLimit() throws Exception {
        // payload 上限设为 1KiB
        final int payload = 1024;
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12389)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
            .setPayload(payload)
            .setDaemon(true);

        HttpService ref = new HttpServiceImpl() {
            @Override
            public EchoResponse echoPb(EchoRequest request) {
                invoked.incrementAndGet();
                return super.echoPb(request);
            }
        };
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(ref)
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        ClientTransportConfig clientTransportConfig = new ClientTransportConfig();
        clientTransportConfig.setProviderInfo(ProviderHelper.toProviderInfo("h2c://127.0.0.1:12389"));
        Http2ClientTransport clientTransport = new Http2ClientTransport(clientTransportConfig);
        clientTransport.connect();

        // 1) 累计长度低于限制的合法请求应当正常处理
        {
            FullHttpRequest httpRequest = buildValidRequest();
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertNotNull("small request should get a response", handler.response);
            Assert.assertEquals(200, handler.response.status().code());
            Assert.assertEquals("service method should be invoked once for the small request", 1, invoked.get());
        }

        // 2) 累计长度超过限制的请求：必须重置/拒绝，业务方法不得被调用
        int before = invoked.get();
        {
            FullHttpRequest httpRequest = buildOversizedRequest(payload * 2);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            // 不应收到 200 正常响应：要么无响应（流被重置/连接关闭），要么以异常告终
            boolean got200 = handler.response != null && handler.response.status().code() == 200;
            Assert.assertFalse("oversized request must not produce a 200 response", got200);
            Assert.assertEquals("service method must not be invoked for oversized request", before, invoked.get());
        }
    }

    /** 构造一个体积远小于 payload 上限的合法 protobuf 请求。 */
    private FullHttpRequest buildValidRequest() {
        EchoRequest request = EchoRequest.newBuilder().setName("xxx").build();
        return buildRequest(request.toByteArray());
    }

    /** 构造一个体积超过 payload 上限的请求（任意字节填充，仅用于触发体积上限）。 */
    private FullHttpRequest buildOversizedRequest(int bodySize) {
        return buildRequest(new byte[bodySize]);
    }

    private FullHttpRequest buildRequest(byte[] body) {
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST,
            "http://127.0.0.1:12389/com.alipay.sofa.rpc.server.http.HttpService/echoPb",
            wrappedBuffer(body));
        HttpHeaders headers = httpRequest.headers();
        headers.add(HttpHeaderNames.HOST, "127.0.0.1");
        headers.add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), "HTTP");
        headers.add(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
        return httpRequest;
    }

    private MyHandler sendHttpRequest(Http2ClientTransport clientTransport, FullHttpRequest httpRequest)
        throws InterruptedException {
        MyHandler handler = new MyHandler();
        int requestId = clientTransport.sendHttpRequest(httpRequest, handler);
        handler.latch.await(3, TimeUnit.SECONDS);
        // 服务端重置 stream 时不会回响应，handler 不会被回调，需主动清理待响应 promise，避免泄漏到后续测试
        clientTransport.responseChannelHandler.removePromise(requestId);
        return handler;
    }

    private final class MyHandler extends AbstractHttpClientHandler {

        CountDownLatch   latch = new CountDownLatch(1);
        FullHttpResponse response;
        Throwable        exception;

        protected MyHandler() {
            super(null, null, null, null, null);
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void doOnResponse(Object result) {
            latch.countDown();
        }

        @Override
        public void doOnException(Throwable e) {
            this.exception = e;
            latch.countDown();
        }

        @Override
        public void receiveHttpResponse(FullHttpResponse response) {
            this.response = response;
            latch.countDown();
        }
    }
}