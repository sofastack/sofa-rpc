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

import com.alipay.sofa.rpc.transport.triple.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * HTTP/1.1 input message implementation.
 */
public class Http1InputMessage implements HttpInputMessage {

    private byte[]  body;
    private boolean endOfStream;

    public Http1InputMessage() {
    }

    public Http1InputMessage(byte[] body) {
        this.body = body;
        this.endOfStream = true;
    }

    public Http1InputMessage(byte[] body, boolean endOfStream) {
        this.body = body;
        this.endOfStream = endOfStream;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public InputStream getBodyStream() {
        return body != null ? new ByteArrayInputStream(body) : null;
    }

    @Override
    public boolean isEndOfStream() {
        return endOfStream;
    }

    // Setter

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setEndOfStream(boolean endOfStream) {
        this.endOfStream = endOfStream;
    }

    @Override
    public String toString() {
        return "Http1InputMessage{" +
            "bodyLength=" + (body != null ? body.length : 0) +
            ", endOfStream=" + endOfStream +
            '}';
    }
}