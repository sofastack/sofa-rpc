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

import com.alipay.sofa.rpc.transport.triple.http.HttpOutputMessage;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * HTTP/1.1 output message implementation.
 */
public class Http1OutputMessage implements HttpOutputMessage {

    private byte[]                body;
    private ByteArrayOutputStream outputStream;

    public Http1OutputMessage() {
    }

    public Http1OutputMessage(byte[] body) {
        this.body = body;
    }

    @Override
    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public OutputStream getBodyStream() {
        if (outputStream == null) {
            outputStream = new ByteArrayOutputStream();
        }
        return outputStream;
    }

    @Override
    public byte[] getBody() {
        if (body != null) {
            return body;
        }
        if (outputStream != null) {
            return outputStream.toByteArray();
        }
        return null;
    }

    @Override
    public String toString() {
        return "Http1OutputMessage{" +
            "bodyLength=" + (body != null ? body.length : 0) +
            '}';
    }
}