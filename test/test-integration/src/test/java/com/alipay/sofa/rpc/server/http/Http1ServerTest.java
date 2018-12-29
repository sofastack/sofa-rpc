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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http1ServerTest extends ActivelyDestroyTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testHttp1General() throws Exception {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12300)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_HTTP)
            .setDaemon(true);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(new HttpServiceImpl())
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setUniqueId("uuu")
            .setRegister(false);
        providerConfig.export();

        HttpClient httpclient = HttpClientBuilder.create().build();

        { // GET 图标
            String url = "http://127.0.0.1:12300/favicon.ico";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            Assert.assertTrue(StringUtils.isEmpty(getStringContent(httpResponse)));
        }
        { // 其它未知命令
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/add";
            HttpOptions httpGet = new HttpOptions(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertTrue(StringUtils.isNotEmpty(getStringContent(httpResponse)));
        }

        { // HEAD 不存在的服务
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/add";
            HttpHead httpHead = new HttpHead(url);
            HttpResponse httpResponse = httpclient.execute(httpHead);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
        }
        { // HEAD 不存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/xasdasdasd";
            HttpHead httpHead = new HttpHead(url);
            HttpResponse httpResponse = httpclient.execute(httpHead);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
        }
        { // HEAD 存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/add";
            HttpHead httpHead = new HttpHead(url);
            HttpResponse httpResponse = httpclient.execute(httpHead);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        }

        { // GET 异常：地址不对
            String url = "http://127.0.0.1:12300/com.alipay.sofa";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // GET 不存在的接口
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/asdasdas?code=xxx";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertTrue(getStringContent(httpResponse).contains("asdasdas"));
        }
        { // GET 不存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/asdasdas?code=xxx";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertTrue(getStringContent(httpResponse).contains("asdasdas"));
        }
        { // GET 异常：参数数量不对
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/query";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // GET 异常：参数数量不对
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/add?code=1";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // GET 异常：参数类型不对
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/query?code=xxx";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // GET 正确
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/add?code=1&name=22";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpclient.execute(httpGet);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            Assert.assertEquals("221", getStringContent(httpResponse));
        }
        { // POST 未知序列化
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/adasdad";
            HttpPost httpPost = new HttpPost(url);
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            ByteArrayEntity entity = new ByteArrayEntity(request.toByteArray(), null);
            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(400, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
    }

    @Test
    public void testHttp1Protobuf() throws Exception {

        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12300)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_HTTP)
            .setDaemon(true);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(new HttpServiceImpl())
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setUniqueId("uuu")
            .setRegister(false);
        providerConfig.export();

        HttpClient httpclient = HttpClientBuilder.create().build();

        { // POST 不存在的接口
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/adasdad";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            ByteArrayEntity entity = new ByteArrayEntity(request.toByteArray(),
                ContentType.create("application/protobuf"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // POST 不存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/adasdad";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            ByteArrayEntity entity = new ByteArrayEntity(request.toByteArray(),
                ContentType.create("application/protobuf"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }

        { // POST 不传 HEAD_SERIALIZE_TYPE
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/echoPb";
            HttpPost httpPost = new HttpPost(url);
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            ByteArrayEntity entity = new ByteArrayEntity(request.toByteArray(),
                ContentType.create("application/protobuf"));
            httpPost.setEntity(entity);

            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            byte[] data = EntityUtils.toByteArray(httpResponse.getEntity());

            EchoResponse response = EchoResponse.parseFrom(data);
            Assert.assertEquals("helloxxx", response.getMessage());
        }

        { // POST 正常请求
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/echoPb";
            HttpPost httpPost = new HttpPost(url);
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "protobuf");
            ByteArrayEntity entity = new ByteArrayEntity(request.toByteArray(),
                ContentType.create("application/protobuf"));
            httpPost.setEntity(entity);

            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            byte[] data = EntityUtils.toByteArray(httpResponse.getEntity());

            EchoResponse response = EchoResponse.parseFrom(data);
            Assert.assertEquals("helloxxx", response.getMessage());
        }

    }

    @Test
    public void testHttp1Json() throws Exception {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12300)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_HTTP)
            .setDaemon(true);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(new HttpServiceImpl())
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setUniqueId("uuu")
            .setRegister(false);
        providerConfig.export();

        HttpClient httpclient = HttpClientBuilder.create().build();

        { // POST jackson不存在的接口
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService/adasdad";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "json");
            ExampleObj obj = new ExampleObj();
            obj.setId(1);
            obj.setName("xxx");
            byte[] bytes = mapper.writeValueAsBytes(obj);
            ByteArrayEntity entity = new ByteArrayEntity(bytes,
                ContentType.create("application/json"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }
        { // POST 不存在的方法
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/adasdad";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "json");
            ExampleObj obj = new ExampleObj();
            obj.setId(1);
            obj.setName("xxx");
            byte[] bytes = mapper.writeValueAsBytes(obj);
            ByteArrayEntity entity = new ByteArrayEntity(bytes,
                ContentType.create("application/json"));

            httpPost.setEntity(entity);
            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(404, httpResponse.getStatusLine().getStatusCode());
            Assert.assertNotNull(getStringContent(httpResponse));
        }

        { // POST 不传 HEAD_SERIALIZE_TYPE
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/object";
            HttpPost httpPost = new HttpPost(url);
            ExampleObj obj = new ExampleObj();
            obj.setId(1);
            obj.setName("xxx");
            byte[] bytes = mapper.writeValueAsBytes(obj);
            ByteArrayEntity entity = new ByteArrayEntity(bytes,
                ContentType.create("application/json"));

            httpPost.setEntity(entity);

            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            byte[] data = EntityUtils.toByteArray(httpResponse.getEntity());

            ExampleObj result = mapper.readValue(data, ExampleObj.class);
            Assert.assertEquals("xxxxx", result.getName());
        }

        { // POST 正常请求
            String url = "http://127.0.0.1:12300/com.alipay.sofa.rpc.server.http.HttpService:uuu/object";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(RemotingConstants.HEAD_SERIALIZE_TYPE, "json");
            ExampleObj obj = new ExampleObj();
            obj.setId(1);
            obj.setName("xxx");
            byte[] bytes = mapper.writeValueAsBytes(obj);
            ByteArrayEntity entity = new ByteArrayEntity(bytes,
                ContentType.create("application/json"));

            httpPost.setEntity(entity);

            HttpResponse httpResponse = httpclient.execute(httpPost);
            Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
            byte[] data = EntityUtils.toByteArray(httpResponse.getEntity());

            ExampleObj result = mapper.readValue(data, ExampleObj.class);

            Assert.assertEquals("xxxxx", result.getName());
        }
    }

    private String getStringContent(HttpResponse httpResponse) throws IOException {
        return EntityUtils.toString(httpResponse.getEntity());
    }

    @After
    public void afterMethod() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }
}
