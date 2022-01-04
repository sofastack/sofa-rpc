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

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.test.HelloService;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

/**
 * @author BaoYi
 * @date 2021/12/26 3:26 PM
 */
public class CustomSetterFactory implements SetterFactory {

    @Override
    public HystrixCommand.Setter createSetter(FilterInvoker invoker, SofaRequest request) {
        return HystrixCommand.Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(HelloService.class.getSimpleName()))
            .andCommandKey(HystrixCommandKey.Factory.asKey("sendMsg"));
    }
}
