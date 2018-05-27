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
package com.alipay.sofa.rpc.protobuf;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtoServiceImpl implements ProtoService {

    private int           sleep;

    private String        result;

    private AtomicInteger counter = new AtomicInteger();

    public ProtoServiceImpl() {

    }

    public ProtoServiceImpl(String result) {
        this.result = result;
    }

    public ProtoServiceImpl(int sleep) {
        this.sleep = sleep;
    }

    @Override
    public EchoResponse echoObj(EchoRequest req) {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (Exception ignore) { // NOPMD
            }
        }
        counter.incrementAndGet();
        EchoResponse response = EchoResponse.newBuilder()
            .setCode(200)
            .setMessage(result != null ? result : "protobuf works! " + req.getName())
            .build();
        return response;
    }

    public AtomicInteger getCounter() {
        return counter;
    }
}
