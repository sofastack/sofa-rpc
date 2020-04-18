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
package com.alipay.sofa.rpc.hystrix;

import java.util.concurrent.atomic.AtomicInteger;

public class InvokeCounterHystrixService implements HystrixService {

    private int           sleep;

    private String        result;

    private AtomicInteger executeCount = new AtomicInteger(0);

    public InvokeCounterHystrixService(int sleep) {
        this.sleep = sleep;
    }

    public InvokeCounterHystrixService(String result) {
        this.result = result;
    }

    @Override
    public String sayHello(String name, int age) {
        executeCount.incrementAndGet();
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (Exception ignore) {
            }
        }
        return result != null ? result : "hello " + name + " from server! age: " + age;
    }

    public int getExecuteCount() {
        return executeCount.get();
    }
}
