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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RestTest extends ActivelyDestroyTest {

    @Test
    public void testAll() throws InterruptedException {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(8802)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
            .setContextPath("/xyz");
        //.setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("rest")
            //.setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<RestService> consumerConfig = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setDirectUrl(
                "rest://127.0.0.1:8802/xyz?uniqueId=&version=1.0&timeout=0&delay=-1&id=rpc-cfg-0&dynamic=true&weight=100&accepts=100000&startTime=1523240755024&appName=test-server&pid=22385&language=java&rpcVer=50300")
            .setProtocol("rest")
            .setBootstrap("rest")
            .setTimeout(30000)
            .setConnectionNum(5)
            .setRegister(false);
        final RestService helloService = consumerConfig.refer();

        Assert.assertEquals(helloService.query(11), "hello world !null");

        int times = 5;
        final CountDownLatch latch = new CountDownLatch(times);
        final AtomicInteger count = new AtomicInteger();

        // 瞬间发起5个请求，那么服务端肯定在排队
        for (int i = 0; i < times; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        helloService.add(22, "xxx");
                        count.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }, "thread" + i);
            thread.start();
            System.out.println("send " + i);
            try {
                Thread.sleep(200);
            } catch (Exception ignore) {
            }
        }

        latch.await(3, TimeUnit.SECONDS);
        // 应该执行了5个请求
        Assert.assertTrue(count.get() == 5);

        Assert.assertEquals(helloService.add(11, "yyy"), "create ok !11");
        Assert.assertEquals(helloService.query(11), "hello world !yyy");
        Assert.assertEquals(helloService.query(11, "xxx"), "hello world !yyy");
        Assert.assertEquals(helloService.update(11, "xxx").readEntity(String.class), "update ok !11");
        Assert.assertEquals(helloService.query(11), "hello world !xxx");
        Assert.assertEquals(helloService.delete(11), "xxx");

        boolean error = false;
        try {
            helloService.error("11");
        } catch (Exception e) {
            error = true;
            Assert.assertTrue(e instanceof SofaRpcException);
        }
        Assert.assertTrue(error);

        ExampleObj obj = new ExampleObj();
        obj.setId(11);
        obj.setName("name");
        ExampleObj serverobk = helloService.object(obj);
        Assert.assertEquals(serverobk.getId(), obj.getId());
        Assert.assertEquals(serverobk.getName(), obj.getName() + " server");

        ExampleObj obj2 = new ExampleObj();
        obj2.setId(22);
        obj2.setName("name22");
        List<ExampleObj> objs = helloService.objects(Arrays.asList(obj, obj2));
        Assert.assertEquals(objs.size(), 2);
        Assert.assertEquals(objs.get(1).getId(), obj2.getId());
        Assert.assertEquals(objs.get(1).getName(), obj2.getName() + " server");

        Assert.assertEquals(helloService.get("zzz"), "serverzzz");
        Assert.assertEquals(helloService.post("zzz", "boddddy"), "server zzzboddddy");

        providerConfig.unExport();
    }
}
