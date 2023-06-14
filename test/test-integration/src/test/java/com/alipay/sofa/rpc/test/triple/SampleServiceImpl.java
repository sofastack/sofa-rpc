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
package com.alipay.sofa.rpc.test.triple;

/**
 * @author Even
 * @date 2023/6/13 11:33 AM
 */
public class SampleServiceImpl implements SampleService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String messageSize(String msg, int responseMessageSize) {
        if (responseMessageSize > 0) {
            StringBuilder sb = new StringBuilder();
            /* 1KB */
            for (int i = 0; i < responseMessageSize * 1024; i++) {
                sb.append('a');
            }
            return sb.toString();
        }
        return msg;
    }

}
