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
package com.alipay.sofa.rpc.transport.triple.http2;

import com.alipay.sofa.rpc.transport.triple.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * HTTP/2 implementation of HttpInputMessage.
 * Wraps byte array data from HTTP/2 data frames.
 */
public class Http2InputMessage implements HttpInputMessage {

    private final byte[] data;
    private boolean      endOfStream = true;

    /**
     * Create a new Http2InputMessage.
     *
     * @param data the input data
     */
    public Http2InputMessage(byte[] data) {
        this.data = data;
    }

    /**
     * Create a new Http2InputMessage with end-of-stream flag.
     *
     * @param data the input data
     * @param endOfStream whether this is the last message
     */
    public Http2InputMessage(byte[] data, boolean endOfStream) {
        this.data = data;
        this.endOfStream = endOfStream;
    }

    @Override
    public byte[] getBody() {
        return data;
    }

    @Override
    public InputStream getBodyStream() {
        return data != null ? new ByteArrayInputStream(data) : null;
    }

    @Override
    public boolean isEndOfStream() {
        return endOfStream;
    }

    /**
     * Set end of stream flag.
     *
     * @param endOfStream flag
     */
    public void setEndOfStream(boolean endOfStream) {
        this.endOfStream = endOfStream;
    }

    @Override
    public void close() {
        // No-op for byte array
    }
}