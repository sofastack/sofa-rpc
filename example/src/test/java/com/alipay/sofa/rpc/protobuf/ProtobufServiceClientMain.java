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

import com.alipay.sofa.rpc.config.ConsumerConfig;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtobufServiceClientMain {

    public static void main(String[] args) {
        ConsumerConfig<ProtoService> consumerConfig = new ConsumerConfig<ProtoService>()
            .setInterfaceId(ProtoService.class.getName()) // 指定接口
            .setProtocol("bolt") // 指定协议
            .setDirectUrl("bolt://127.0.0.1:12200") // 指定直连地址
            .setSerialization("protobuf") // 指定序列化协议，默认为hessian
            .setConnectTimeout(10 * 1000);

        ProtoService helloService = consumerConfig.refer();

        while (true) {
            try {
                EchoRequest request = EchoRequest.newBuilder().setName("zhang").setGroup(Group.A).build();
                EchoResponse response = helloService.echoObj(request);
                System.out.println(response.getCode() + ": " + response.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
