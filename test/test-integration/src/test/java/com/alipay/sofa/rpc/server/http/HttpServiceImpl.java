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
package com.alipay.sofa.rpc.server.http;

import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;

import java.util.List;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class HttpServiceImpl implements HttpService {

    private int sleep;

    public HttpServiceImpl() {

    }

    public HttpServiceImpl(int sleep) {
        this.sleep = sleep;
    }

    @Override
    public String add(int code, String name) {
        doSleep();
        return name + code;
    }

    @Override
    public void noResp() {
        doSleep();
    }

    @Override
    public String query(int code) {
        doSleep();
        return "xx" + code;
    }

    @Override
    public ExampleObj object(ExampleObj code) {
        doSleep();
        code.setName(code.getName() + "xx");
        return code;
    }

    @Override
    public List<ExampleObj> objects(List<ExampleObj> code) {
        doSleep();
        for (ExampleObj exampleObj : code) {
            object(exampleObj);
        }
        return code;
    }

    @Override
    public String error(String code) {
        doSleep();
        throw new RuntimeException("xxx");
    }

    @Override
    public EchoResponse echoPb(EchoRequest request) {
        doSleep();
        if (request.getGroup().equals(Group.B)) {
            throw new RuntimeException("group must is a!");
        }
        return EchoResponse.newBuilder()
            .setCode(200)
            .setMessage("hello" + request.getName())
            .build();
    }

    private void doSleep() {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (Exception e) { // NOPMD
            }
        }
    }
}
