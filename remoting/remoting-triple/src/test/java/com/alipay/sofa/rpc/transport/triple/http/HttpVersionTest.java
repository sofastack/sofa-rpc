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
package com.alipay.sofa.rpc.transport.triple.http;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link HttpVersion}.
 */
public class HttpVersionTest {

    @Test
    public void testHttp1Values() {
        Assert.assertEquals("http1", HttpVersion.HTTP_1.getVersion());
        Assert.assertEquals("HTTP/1.1", HttpVersion.HTTP_1.getProtocol());
    }

    @Test
    public void testHttp2Values() {
        Assert.assertEquals("http2", HttpVersion.HTTP_2.getVersion());
        Assert.assertEquals("HTTP/2.0", HttpVersion.HTTP_2.getProtocol());
    }

    @Test
    public void testHttp3Values() {
        Assert.assertEquals("http3", HttpVersion.HTTP_3.getVersion());
        Assert.assertEquals("HTTP/3.0", HttpVersion.HTTP_3.getProtocol());
    }

    @Test
    public void testFromVersionHttp1() {
        Assert.assertEquals(HttpVersion.HTTP_1, HttpVersion.fromVersion("http1"));
        Assert.assertEquals(HttpVersion.HTTP_1, HttpVersion.fromVersion("HTTP/1.1"));
        Assert.assertEquals(HttpVersion.HTTP_1, HttpVersion.fromVersion("1.1"));
        Assert.assertEquals(HttpVersion.HTTP_1, HttpVersion.fromVersion("1"));
    }

    @Test
    public void testFromVersionHttp2() {
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("http2"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("HTTP/2.0"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("HTTP/2"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("2.0"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("2"));
    }

    @Test
    public void testFromVersionHttp3() {
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("http3"));
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("HTTP/3.0"));
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("HTTP/3"));
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("3.0"));
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("3"));
    }

    @Test
    public void testFromVersionNull() {
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion(null));
    }

    @Test
    public void testFromVersionUnknown() {
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("unknown"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion(""));
    }

    @Test
    public void testFromVersionCaseInsensitive() {
        Assert.assertEquals(HttpVersion.HTTP_1, HttpVersion.fromVersion("HTTP1"));
        Assert.assertEquals(HttpVersion.HTTP_2, HttpVersion.fromVersion("HTTP2"));
        Assert.assertEquals(HttpVersion.HTTP_3, HttpVersion.fromVersion("HTTP3"));
    }
}