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
package com.alipay.sofa.rpc.registry.etcd.client;

import com.alipay.sofa.rpc.registry.etcd.grpc.api.KeyValue;

import java.util.List;
import java.util.UUID;

/**
 * @author Fuwenming
 * @created 2018/6/5
 **/
public class Main {

    public static void main(String[] args) throws InterruptedException {
        final EtcdClient client = EtcdClient.builder().endpoints("127.0.0.1")
            //            .auth("root", "root123")
            .build();

        List<KeyValue> keyValues = client.getWithPrefix("key");
        for (int i = 0; i < keyValues.size(); i++) {
            KeyValue keyValue = keyValues.get(i);
        }

        //        for (int i = 0; i < 10; i++) {
        Long id = client
            .putWithLease("key-with-uuid:111", "some value for key");
        client.keepAlive(id);
        //        }

        while (true) {
            List<KeyValue> keyValues1 = client.getWithPrefix("key");
            for (int i = 0; i < keyValues1.size(); i++) {
                KeyValue keyValue = keyValues1.get(i);
                System.out
                    .println(System.currentTimeMillis() + ":" +
                        keyValue.getKey().toStringUtf8() + " - " + keyValue.getValue().toStringUtf8());
            }
            Thread.sleep(5000);

            for (int i = 0; i < keyValues1.size(); i++) {
                if (i % 5 == 0) {
                    KeyValue keyValue = keyValues1.get(i);
                    client.revokeLease(keyValue.getLease());
                }

            }
        }

    }

}
