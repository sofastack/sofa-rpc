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
package com.alipay.sofa.rpc.server.bolt;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltProtobufTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        ProviderConfig<ProtobufService> providerConfig = new ProviderConfig<ProtobufService>()
            .setInterfaceId(ProtobufService.class.getName()) // 指定接口
            .setRef(new ProtobufServiceImpl()) // 指定实现
            .setServer(serverConfig); // 指定服务端
        providerConfig.export(); // 发布服务

        ConsumerConfig<ProtobufService> consumerConfig = new ConsumerConfig<ProtobufService>()
            .setInterfaceId(ProtobufService.class.getName()) // 指定接口
            .setProtocol("bolt") // 指定协议
            .setDirectUrl("bolt://127.0.0.1:12200") // 指定直连地址
            .setSerialization("protobuf") // 指定序列化协议，默认为hessian
            .setConnectTimeout(10 * 1000);
        ProtobufService helloService = consumerConfig.refer();

        EchoRequest request = EchoRequest.newBuilder().setName("sofa").setGroup(Group.A).build();
        EchoResponse response = helloService.echoObj(request);
        System.out.println(response.getCode() + ": " + response.getMessage());

        boolean error = false;
        try {
            helloService.echoObj(null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Assert.assertEquals(200, response.getCode());
        Assert.assertEquals("protobuf works! sofa", response.getMessage());
    }
}
