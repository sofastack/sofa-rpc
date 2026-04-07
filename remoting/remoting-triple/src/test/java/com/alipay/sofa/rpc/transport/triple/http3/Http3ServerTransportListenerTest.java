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
package com.alipay.sofa.rpc.transport.triple.http3;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.triple.http.DefaultHttpHeaders;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for HTTP/3 server transport listener.
 */
public class Http3ServerTransportListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http3ServerTransportListenerTest.class);

    @Test
    public void testHttp3Metadata() {
        LOGGER.info("Testing HTTP/3 metadata creation and accessors");

        Http3Metadata metadata = new Http3Metadata("POST", "/helloworld.Greeter/sayHello");

        // Verify basic properties
        Assert.assertEquals("Method should be POST", "POST", metadata.method());
        Assert.assertEquals("Path should match", "/helloworld.Greeter/sayHello", metadata.path());
        Assert.assertEquals("HTTP version should be HTTP/3", HttpVersion.HTTP_3, metadata.httpVersion());
        Assert.assertEquals("Default scheme should be https", "https", metadata.scheme());

        // Test headers
        metadata.headers().set("content-type", "application/grpc");
        Assert.assertEquals("Content-type header should match", "application/grpc",
            metadata.headers().get("content-type"));
        Assert.assertTrue("Headers should contain content-type", metadata.headers().contains("content-type"));

        LOGGER.info("HTTP/3 metadata test passed");
    }

    @Test
    public void testHttp3InputMessage() {
        LOGGER.info("Testing HTTP/3 input message creation and accessors");

        byte[] data = "test data".getBytes();
        Http3InputMessage message = new Http3InputMessage(data);

        // Verify message properties
        Assert.assertArrayEquals("Body should match", data, message.getBody());
        Assert.assertTrue("Default endOfStream should be true", message.isEndOfStream());
        Assert.assertTrue("Should have body", message.hasBody());
        Assert.assertEquals("Content length should match", data.length, message.contentLength());

        // Test with endOfStream flag
        Http3InputMessage message2 = new Http3InputMessage(data, false);
        Assert.assertFalse("endOfStream should be false", message2.isEndOfStream());

        LOGGER.info("HTTP/3 input message test passed");
    }

    @Test
    public void testHttp3MetadataWithSetters() {
        LOGGER.info("Testing HTTP/3 metadata setters");

        Http3Metadata metadata = new Http3Metadata();
        metadata.setMethod("GET");
        metadata.setPath("/test/path");
        metadata.setScheme("http");
        metadata.setRemoteAddress("127.0.0.1:12345");
        metadata.setLocalAddress("127.0.0.1:8080");

        Assert.assertEquals("Method should be GET", "GET", metadata.method());
        Assert.assertEquals("Path should match", "/test/path", metadata.path());
        Assert.assertEquals("Scheme should be http", "http", metadata.scheme());
        Assert.assertEquals("Remote address should match", "127.0.0.1:12345", metadata.remoteAddress());
        Assert.assertEquals("Local address should match", "127.0.0.1:8080", metadata.localAddress());

        LOGGER.info("HTTP/3 metadata setters test passed");
    }

    @Test
    public void testHttp3MetadataToString() {
        LOGGER.info("Testing HTTP/3 metadata toString");

        Http3Metadata metadata = new Http3Metadata("POST", "/test");
        String str = metadata.toString();

        Assert.assertNotNull("toString should not be null", str);
        Assert.assertTrue("toString should contain method", str.contains("POST"));
        Assert.assertTrue("toString should contain path", str.contains("/test"));
        Assert.assertTrue("toString should contain HTTP version", str.contains("HTTP_3"));

        LOGGER.info("HTTP/3 metadata toString test passed");
    }

    @Test
    public void testHttp3InputMessageToString() {
        LOGGER.info("Testing HTTP/3 input message toString");

        Http3InputMessage message = new Http3InputMessage("test".getBytes());
        String str = message.toString();

        Assert.assertNotNull("toString should not be null", str);
        Assert.assertTrue("toString should contain body length", str.contains("bodyLength=4"));
        Assert.assertTrue("toString should contain endOfStream", str.contains("endOfStream=true"));

        LOGGER.info("HTTP/3 input message toString test passed");
    }

    @Test
    public void testHttp3InputMessageSetters() {
        LOGGER.info("Testing HTTP/3 input message setters");

        Http3InputMessage message = new Http3InputMessage();
        message.setBody("new data".getBytes());
        message.setEndOfStream(false);

        Assert.assertArrayEquals("Body should match", "new data".getBytes(), message.getBody());
        Assert.assertFalse("endOfStream should be false", message.isEndOfStream());

        LOGGER.info("HTTP/3 input message setters test passed");
    }

    @Test
    public void testHttp3MetadataHeaders() {
        LOGGER.info("Testing HTTP/3 metadata headers operations");

        Http3Metadata metadata = new Http3Metadata("POST", "/test");

        // Test multiple headers
        metadata.headers().set("content-type", "application/grpc");
        metadata.headers().set("grpc-timeout", "10S");
        metadata.headers().set("grpc-encoding", "gzip");

        Assert.assertEquals("Content-type should match", "application/grpc", metadata.headers().get("content-type"));
        Assert.assertEquals("Timeout should match", "10S", metadata.headers().get("grpc-timeout"));
        Assert.assertEquals("Encoding should match", "gzip", metadata.headers().get("grpc-encoding"));
        Assert.assertEquals("Headers count should be 3", 3, metadata.headers().size());
        Assert.assertFalse("Headers should not be empty", metadata.headers().isEmpty());

        // Test remove
        metadata.headers().remove("grpc-encoding");
        Assert.assertFalse("Headers should not contain removed key", metadata.headers().contains("grpc-encoding"));
        Assert.assertEquals("Headers count should be 2 after removal", 2, metadata.headers().size());

        LOGGER.info("HTTP/3 metadata headers test passed");
    }

    @Test
    public void testHttp3InputMessageEmptyBody() {
        LOGGER.info("Testing HTTP/3 input message with empty body");

        Http3InputMessage message = new Http3InputMessage(new byte[0]);

        Assert.assertNotNull("Body should not be null", message.getBody());
        Assert.assertEquals("Body length should be 0", 0, message.getBody().length);
        Assert.assertFalse("Should not have body", message.hasBody());
        Assert.assertEquals("Content length should be 0", 0, message.contentLength());

        LOGGER.info("HTTP/3 input message empty body test passed");
    }
}