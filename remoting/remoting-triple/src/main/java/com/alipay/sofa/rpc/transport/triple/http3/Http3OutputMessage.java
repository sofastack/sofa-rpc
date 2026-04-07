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

import com.alipay.sofa.rpc.transport.triple.http.HttpOutputMessage;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * HTTP/3 implementation of HttpOutputMessage.
 * Used for writing HTTP/3 response data.
 */
public class Http3OutputMessage implements HttpOutputMessage {

    private byte[]                body;
    private ByteArrayOutputStream outputStream;

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
    public void close() {
        // No-op for ByteArrayOutputStream
    }
}