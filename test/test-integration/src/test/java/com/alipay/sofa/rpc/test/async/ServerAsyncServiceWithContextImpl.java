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
package com.alipay.sofa.rpc.test.async;

import com.alipay.sofa.rpc.context.AsyncContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service implementation for testing AsyncContext.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ServerAsyncServiceWithContextImpl implements ServerAsyncServiceWithContext {

    private static final Logger          LOGGER   = LoggerFactory.getLogger(ServerAsyncServiceWithContextImpl.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String sayHelloWithAsyncContext(String name) {
        LOGGER.info("sayHelloWithAsyncContext called with name: {}", name);
        // Start async context
        AsyncContext asyncContext = RpcInvokeContext.startAsync();

        // Submit async task
        executor.submit(() -> {
            try {
                // Simulate async business processing
                Thread.sleep(300);
                asyncContext.write("Hello async context, " + name);
            } catch (Exception e) {
                LOGGER.error("Error in async context", e);
                asyncContext.writeError(e);
            }
        });

        // Return null to indicate async handling
        return null;
    }
}