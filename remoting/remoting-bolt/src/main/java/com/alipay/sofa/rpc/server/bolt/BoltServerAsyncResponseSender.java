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
package com.alipay.sofa.rpc.server.bolt;

import com.alipay.remoting.AsyncContext;
import com.alipay.sofa.rpc.context.ServerAsyncResponseSender;
import com.alipay.sofa.rpc.core.response.SofaResponse;

/**
 * Bolt protocol implementation of ServerAsyncResponseSender.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class BoltServerAsyncResponseSender implements ServerAsyncResponseSender {

    /**
     * The underlying Bolt async context
     */
    private final AsyncContext asyncContext;

    /**
     * Flag to track if response has been sent
     */
    private volatile boolean   sent = false;

    /**
     * Constructor
     *
     * @param asyncContext the Bolt async context
     */
    public BoltServerAsyncResponseSender(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    @Override
    public void sendResponse(SofaResponse response) {
        checkState();
        asyncContext.sendResponse(response);
    }

    @Override
    public boolean isSent() {
        return sent;
    }

    /**
     * Check if response has been sent, throw exception if already sent
     */
    private void checkState() {
        if (sent) {
            throw new IllegalStateException("Async response has already been sent");
        }
        sent = true;
    }
}