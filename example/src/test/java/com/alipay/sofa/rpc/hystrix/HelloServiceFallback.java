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

import com.alipay.sofa.rpc.test.HelloService;

/**
 * @author BaoYi
 * @date 2021/12/26 1:19 PM
 */
public class HelloServiceFallback implements HelloService{

    @Override
    public String sayHello(String name, int age) {
        return "fallback " + name + " from server! age: " + age;
    }

    @Override
    public String sendMsg(String msg, Integer waitTime) {
        return "fallback msg from server!: " + msg;
    }

    public Object $invoke(String msg, Integer waitTime) {
        return "fallback from server! error: ";
    }

}
