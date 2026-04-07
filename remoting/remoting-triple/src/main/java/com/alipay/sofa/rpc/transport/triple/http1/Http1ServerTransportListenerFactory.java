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

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.*;

import java.util.concurrent.Executor;

/**
 * Factory for creating HTTP/1.1 server transport listeners.
 */
@Extension("http1")
public class Http1ServerTransportListenerFactory implements HttpServerTransportListenerFactory {

    private UniqueIdInvoker invoker;

    @Override
    public HttpTransportListener<?, ?> newInstance(HttpChannel channel, ServerConfig serverConfig, Executor bizExecutor) {
        return new Http1ServerTransportListener(channel, serverConfig, invoker, bizExecutor);
    }

    @Override
    public boolean supports(HttpVersion version) {
        return version == HttpVersion.HTTP_1;
    }

    @Override
    public boolean supportsContentType(String contentType) {
        // HTTP/1.1 supports JSON, form data, and other non-gRPC content types
        if (contentType == null) {
            return true;
        }
        // Don't handle gRPC content types
        return !contentType.startsWith("application/grpc");
    }

    @Override
    public int getPriority() {
        // Lower priority than gRPC
        return -100;
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_1;
    }

    /**
     * Set the invoker.
     *
     * @param invoker UniqueIdInvoker
     */
    public void setInvoker(UniqueIdInvoker invoker) {
        this.invoker = invoker;
    }
}