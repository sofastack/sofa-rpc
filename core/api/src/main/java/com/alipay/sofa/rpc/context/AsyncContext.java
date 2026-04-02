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
package com.alipay.sofa.rpc.context;

/**
 * Server-side async context for manual control of response timing.
 * Similar to Dubbo's AsyncContext, allows business code to control when to send response.
 *
 * <p>Usage example:
 * <pre>
 * public class HelloServiceImpl implements HelloService {
 *     public String sayHello(String name) {
 *         AsyncContext asyncContext = RpcInvokeContext.startAsync();
 *         executorService.submit(() -> {
 *             try {
 *                 Thread.sleep(1000);
 *                 asyncContext.write("Hello, " + name);
 *             } catch (Exception e) {
 *                 asyncContext.writeError(e);
 *             }
 *         });
 *         return null;
 *     }
 * }
 * </pre>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface AsyncContext {

    /**
     * Write the async response to the client.
     *
     * @param response the response object
     */
    void write(Object response);

    /**
     * Write error to the client.
     *
     * @param throwable the error
     */
    void writeError(Throwable throwable);

    /**
     * Check if the response has been sent.
     *
     * @return true if response has been sent
     */
    boolean isSent();
}