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
package com.alipay.sofa.rpc.metrics.lookout;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class LookoutServiceImpl implements LookoutService {

    private final AtomicInteger countSync     = new AtomicInteger();
    private final AtomicInteger countFuture   = new AtomicInteger();
    private final AtomicInteger countCallback = new AtomicInteger();
    private final AtomicInteger countOneway   = new AtomicInteger();

    @Override
    public String saySync(String string) throws InterruptedException {
        if (countSync.incrementAndGet() == 3) {
            Thread.sleep(3500);
            throw new RuntimeException();
        } else {
            return string;
        }
    }

    @Override
    public String sayFuture(String string) throws InterruptedException {
        if (countFuture.incrementAndGet() == 4) {
            Thread.sleep(3500);
            throw new RuntimeException();
        } else {
            return string;
        }
    }

    @Override
    public String sayCallback(String string) throws InterruptedException {
        if (countCallback.incrementAndGet() == 5) {
            Thread.sleep(3500);
            throw new RuntimeException();
        } else {
            return string;
        }
    }

    @Override
    public String sayOneway(String string) throws InterruptedException {
        if (countOneway.incrementAndGet() == 6) {
            Thread.sleep(3500);
            throw new RuntimeException();
        } else {
            return string;
        }
    }

}