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
package com.alipay.sofa.rpc.test.generic;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author hongwei.yhw
 * @since 2014-Jul-25
 */
public class TestCallback implements SofaResponseCallback {

    public volatile static Object          result;
    private volatile static CountDownLatch latch;

    @Override
    public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
        result = appResponse;
        int count = 0;
        while (latch == null && count++ < 10) {
            Thread.yield();
        }
        latch.countDown();
    }

    @Override
    public void onAppException(Throwable t, String methodName, RequestBase request) {
        result = t;
        int count = 0;
        while (latch == null && count++ < 10) {
            Thread.yield();
        }
        latch.countDown();
    }

    @Override
    public void onSofaException(SofaRpcException sofaException, String methodName,
                                RequestBase request) {
        result = sofaException;
        latch.countDown();
    }

    public static void startLatach() throws InterruptedException {
        latch = new CountDownLatch(1);
        latch.await(3000, TimeUnit.MILLISECONDS);
    }
}
