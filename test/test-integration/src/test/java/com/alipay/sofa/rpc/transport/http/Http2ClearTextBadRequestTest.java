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
import com.alipay.sofa.rpc.codec.common.StringSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;
import com.alipay.sofa.rpc.server.http.HttpService;
import com.alipay.sofa.rpc.server.http.HttpServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http2ClearTextBadRequestTest extends ActivelyDestroyTest {

    @Test
    public void testAll() throws Exception {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12333)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
            .setDaemon(true);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(new HttpServiceImpl())
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setUniqueId("uuu")
            .setRegister(false);
        providerConfig.export();

        ClientTransportConfig clientTransportConfig = new ClientTransportConfig();
        clientTransportConfig.setProviderInfo(ProviderHelper.toProviderInfo("h2c://127.0.0.1:12333"));
        Http2ClientTransport clientTransport = new Http2ClientTransport(clientTransportConfig);
        clientTransport.connect();

        { // GET 图标
            String url = "/favicon.ico";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(200, handler.response.status().code());
            Assert.assertTrue(handler.content.length == 0);
        }
        { // 其它未知命令
            String url = "/com.alipay.sofa.rpc.server.http.HttpService/add";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.setMethod(HttpMethod.OPTIONS);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(400, handler.response.status().code());
            Assert.assertTrue(StringUtils.isNotEmpty(getStringContent(handler)));
        }

        { // HEAD 不存在的服务
            String url = "/com.alipay.sofa.rpc.server.http.HttpService12313/add";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.setMethod(HttpMethod.HEAD);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(404, handler.response.status().code());
        }
        { // HEAD 不存在的方法
            String url = "/com.alipay.sofa.rpc.server.http.HttpService:uuu/xasdasdasd";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.setMethod(HttpMethod.HEAD);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(404, handler.response.status().code());
        }
        { // HEAD 存在的方法
            String url = "/com.alipay.sofa.rpc.server.http.HttpService:uuu/add";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.setMethod(HttpMethod.HEAD);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(200, handler.response.status().code());
        }

        { // POST 异常：地址不对
            String url = "/com.alipay.sofa";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(400, handler.response.status().code());
            Assert.assertTrue(getStringContent(handler).contains("ip:port"));
        }
        { // POST 未知序列化
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/asdasdas?code=xxx";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(400, handler.response.status().code());
            Assert.assertNotNull(getStringContent(handler));
        }
        { // POST 不存在的接口
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/echoPb";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.headers().add(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(404, handler.response.status().code());
            Assert.assertNotNull(getStringContent(handler));
        }
        { // POST 不存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/adasdada";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.headers().add(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(404, handler.response.status().code());
            Assert.assertNotNull(getStringContent(handler));
        }
        { // POST 不传 HEAD_SERIALIZE_TYPE
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/echoPb";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(200, handler.response.status().code());
            Assert.assertNotNull(getStringContent(handler));
            EchoResponse response = EchoResponse.parseFrom(handler.content);
            Assert.assertEquals("helloxxx", response.getMessage());
        }
        { // POST 正常
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/echoPb";
            FullHttpRequest httpRequest = buildHttpRequest(url);
            httpRequest.headers().add(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            httpRequest.headers().add(RemotingConstants.HEAD_TARGET_APP, "serverApp1");
            MyHandler handler = sendHttpRequest(clientTransport, httpRequest);
            Assert.assertEquals(200, handler.response.status().code());
            EchoResponse response = EchoResponse.parseFrom(handler.content);
            Assert.assertEquals("helloxxx", response.getMessage());
        }
    }

    private FullHttpRequest buildHttpRequest(String url) {
        // Create a simple POST request with a body.
        EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, url,
            wrappedBuffer(request.toByteArray()));
        HttpHeaders headers = httpRequest.headers();
        addToHeader(headers, HttpHeaderNames.HOST, "127.0.0.1");
        addToHeader(headers, HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), "HTTP");
        addToHeader(headers, HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        addToHeader(headers, HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
        return httpRequest;
    }

    private void addToHeader(HttpHeaders headers, CharSequence key, CharSequence value) {
        if (StringUtils.isNotEmpty(value)) {
            headers.add(key, value);
        }
    }

    private MyHandler sendHttpRequest(Http2ClientTransport clientTransport, FullHttpRequest httpRequest)
        throws InterruptedException {
        MyHandler handler = new MyHandler();
        clientTransport.sendHttpRequest(httpRequest, handler);
        handler.latch.await(10000, TimeUnit.MILLISECONDS);
        return handler;
    }

    private String getStringContent(MyHandler handler) {
        byte[] content = handler.content;
        return content == null ? null : StringSerializer.decode(content);
    }

    private final class MyHandler extends AbstractHttpClientHandler {

        CountDownLatch   latch = new CountDownLatch(1);
        FullHttpResponse response;
        Object           result;
        Throwable        exception;
        private byte[]   content;

        protected MyHandler() {
            super(null, null, null, null, null);
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void doOnResponse(Object result) {
            this.result = result;
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
            if (response != null) {
                ByteBuf byteBuf = response.content();
                if (byteBuf.hasArray()) {
                    content = byteBuf.array();
                } else {
                    content = new byte[byteBuf.readableBytes()];
                    byteBuf.readBytes(content);
                }
            }
            latch.countDown();
        }
    }
}
