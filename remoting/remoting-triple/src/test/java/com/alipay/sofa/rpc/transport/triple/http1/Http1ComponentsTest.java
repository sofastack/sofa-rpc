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
package com.alipay.sofa.rpc.transport.triple.http1;

import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for HTTP/1.1 components.
 */
public class Http1ComponentsTest {

    @Test
    public void testHttp1MetadataCreation() {
        Http1Metadata metadata = new Http1Metadata("POST", "/com.example.Service/method");

        Assert.assertEquals("POST", metadata.method());
        Assert.assertEquals("/com.example.Service/method", metadata.path());
        Assert.assertEquals(HttpVersion.HTTP_1, metadata.httpVersion());
    }

    @Test
    public void testHttp1MetadataHeaders() {
        Http1Metadata metadata = new Http1Metadata();
        metadata.setMethod("GET");
        metadata.setPath("/test");
        metadata.headers().set("Content-Type", "application/json");

        Assert.assertEquals("GET", metadata.method());
        Assert.assertEquals("/test", metadata.path());
        Assert.assertEquals("application/json", metadata.contentType());
        Assert.assertFalse(metadata.isGrpcRequest());
        Assert.assertTrue(metadata.isJsonRequest());
    }

    @Test
    public void testHttp1MetadataGrpcDetection() {
        Http1Metadata metadata = new Http1Metadata();
        metadata.headers().set("Content-Type", "application/grpc");

        Assert.assertTrue(metadata.isGrpcRequest());
        Assert.assertFalse(metadata.isJsonRequest());
    }

    @Test
    public void testHttp1InputMessage() {
        byte[] data = "test data".getBytes();
        Http1InputMessage message = new Http1InputMessage(data);

        Assert.assertArrayEquals(data, message.getBody());
        Assert.assertTrue(message.hasBody());
        Assert.assertTrue(message.isEndOfStream());
        Assert.assertEquals(data.length, message.contentLength());
    }

    @Test
    public void testHttp1InputMessageEmpty() {
        Http1InputMessage message = new Http1InputMessage();

        Assert.assertNull(message.getBody());
        Assert.assertFalse(message.hasBody());
    }

    @Test
    public void testHttp1OutputMessage() {
        byte[] data = "test output".getBytes();
        Http1OutputMessage message = new Http1OutputMessage(data);

        Assert.assertArrayEquals(data, message.getBody());
        Assert.assertEquals(data.length, message.contentLength());
    }

    @Test
    public void testHttp1OutputMessageSetBody() {
        Http1OutputMessage message = new Http1OutputMessage();
        byte[] data = "new data".getBytes();
        message.setBody(data);

        Assert.assertArrayEquals(data, message.getBody());
    }
}