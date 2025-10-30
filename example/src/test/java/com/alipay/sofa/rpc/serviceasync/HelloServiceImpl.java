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
package com.alipay.sofa.rpc.serviceasync;

import com.alipay.sofa.rpc.message.bolt.BoltAsyncContext;

public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        BoltAsyncContext boltAsyncContext = new BoltAsyncContext();
        new Thread(() -> {
            // 如果需要在新线程中使用调用上下文，需要调用signalContextSwitch方法
            boltAsyncContext.signalContextSwitch();

            boltAsyncContext.write("Hello " + name);

            boltAsyncContext.resetContext();
        }).start();
        return null;
    }
}
