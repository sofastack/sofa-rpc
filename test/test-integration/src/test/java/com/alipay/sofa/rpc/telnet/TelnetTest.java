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
package com.alipay.sofa.rpc.telnet;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

/**
 * @author zhaowang
 * @version : TelnetTest.java, v 0.1 2021年09月29日 7:46 下午 zhaowang
 */
public class TelnetTest {
    @Before
    public void init() {

        String file = System.getProperty("user.home") + File.separator
            + "localFileTest" + File.separator + "localRegistry.reg";

        RegistryConfig registryConfig = new RegistryConfig().setProtocol("local")
            .setFile(file);

        //发布服务，publishHelloService，发布Hello服务的方法
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setPort(22000)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegistry(registryConfig);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
            .setInterfaceId(EchoService.class.getName())
            .setApplication(application)
            .setRef(new EchoServiceImpl())
            .setServer(serverConfig)
            .setRegistry(registryConfig);

        providerConfig.export();
        providerConfig2.export();

        //引用服务
        ApplicationConfig application2 = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(application2)
            .setInterfaceId(HelloService.class.getName())
            .setRegistry(registryConfig)
            .setTimeout(3000);
        HelloService helloService = consumerConfig.refer();
    }

    @Test
    public void telnet() throws InterruptedException {
        try {
            TelnetClient telnetClient = new TelnetClient("vt200"); //指明Telnet终端类型，否则会返回来的数据中文会乱码
            telnetClient.setDefaultTimeout(5000); //socket延迟时间：5000ms
            telnetClient.connect("127.0.0.1", 1234); //建立一个连接,默认端口是23
            InputStream inputStream = telnetClient.getInputStream(); //读取命令的流
            PrintStream pStream = new PrintStream(telnetClient.getOutputStream()); //写命令的流
            String s = readFromServer(inputStream);
            Assert.assertEquals("sofa-rpc>", s);
            pStream.println("list"); //写命令
            pStream.flush(); //将命令发送到telnet Server
            String result = readFromServer(inputStream);

            Assert.assertTrue(result.contains("PROVIDER"));
            Assert.assertTrue(result.contains("com.alipay.sofa.rpc.test.HelloService"));
            Assert.assertTrue(result.contains("com.alipay.sofa.rpc.test.EchoService"));
            Assert.assertTrue(result.contains("CONSUMER"));
            Assert.assertTrue(result.contains("com.alipay.sofa.rpc.test.HelloService"));
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFromServer(InputStream inputStream) throws IOException {
        byte[] b = new byte[4096];
        StringBuffer sBuffer = new StringBuffer(300);
        int size;
        while (true) { //读取Server返回来的数据，直到读到登陆标识，这个时候认为可以输入用户名
            size = inputStream.read(b);
            if (-1 != size) {
                sBuffer.append(new String(b, 0, size));
                if (sBuffer.toString().trim().endsWith("sofa-rpc>")) {
                    break;
                }
            }
        }
        return sBuffer.toString();
    }

}