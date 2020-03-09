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
package com.alipay.sofa.rpc.registry.mesh;

import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.registry.mesh.client.MeshApiClient;
import com.alipay.sofa.rpc.registry.mesh.mock.HttpMockServer;
import com.alipay.sofa.rpc.registry.mesh.model.ApplicationInfoRequest;
import com.alipay.sofa.rpc.registry.mesh.model.ApplicationInfoResult;
import com.alipay.sofa.rpc.registry.mesh.model.MeshEndpoint;
import com.alipay.sofa.rpc.registry.mesh.model.ProviderMetaInfo;
import com.alipay.sofa.rpc.registry.mesh.model.PublishServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.PublishServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.SubscribeServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.SubscribeServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.UnPublishServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.UnPublishServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.UnSubscribeServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.UnSubscribeServiceResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class MeshApiClientTest extends BaseMeshTest {

    private MeshApiClient  meshApiClient;

    private HttpMockServer httpMockServer;

    @Test
    public void testApplicationInfo() {
        ApplicationInfoRequest request = new ApplicationInfoRequest();
        request.setAppName("test");
        boolean result = meshApiClient.registeApplication(request);
        Assert.assertTrue(result);
    }

    @Before
    public void before() {

        httpMockServer = new HttpMockServer();
        meshApiClient = new MeshApiClient("http://localhost:7654");

        httpMockServer.initSever(7654);
        ApplicationInfoResult applicationInfoResult = new ApplicationInfoResult();
        applicationInfoResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.CONFIGS, JSON.toJSONString(applicationInfoResult));

        PublishServiceResult publishServiceResult = new PublishServiceResult();
        publishServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.PUBLISH, JSON.toJSONString(publishServiceResult));

        SubscribeServiceResult subscribeServiceResult = new SubscribeServiceResult();
        subscribeServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.SUBCRIBE, JSON.toJSONString(subscribeServiceResult));

        UnPublishServiceResult unPublishServiceResult = new UnPublishServiceResult();
        unPublishServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.UN_PUBLISH, JSON.toJSONString(unPublishServiceResult));

        UnSubscribeServiceResult unSubscribeServiceResult = new UnSubscribeServiceResult();
        unSubscribeServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.UN_SUBCRIBE, JSON.toJSONString(unSubscribeServiceResult));
        httpMockServer.start();
    }

    @Test
    public void testPublish() {
        PublishServiceRequest request = new PublishServiceRequest();
        request.setServiceName("aa");
        ProviderMetaInfo providerMetaInfo = new ProviderMetaInfo();
        providerMetaInfo.setAppName("testApp");
        providerMetaInfo.setProtocol("bolt");
        providerMetaInfo.setSerializeType("hessian2");
        providerMetaInfo.setVersion("4.0");
        request.setProviderMetaInfo(providerMetaInfo);
        boolean result = meshApiClient.publishService(request);
        Assert.assertTrue(result);
    }

    @Test
    public void testUnPublish() {
        UnPublishServiceRequest request = new UnPublishServiceRequest();
        request.setServiceName("aa");
        int result = meshApiClient.unPublishService(request);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testSubscribe() {
        //11.166.22.163:12200?_TIMEOUT=3000&p=1&_SERIALIZETYPE=protobuf&_WARMUPTIME=0&_WARMUPWEIGHT=10&app_name=bar1&zone=GZ00A&_MAXREADIDLETIME=30&_IDLETIMEOUT=27&v=4.0&_WEIGHT=100&startTime=1524565802559
        SubscribeServiceRequest request = new SubscribeServiceRequest();
        request.setServiceName("com.alipay.rpc.common.service.facade.pb.SampleServicePb:1.0");
        SubscribeServiceResult result = meshApiClient.subscribeService(request);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void testUnSubscribe() {
        UnSubscribeServiceRequest request = new UnSubscribeServiceRequest();
        request.setServiceName("com.alipay.rpc.common.service.facade.pb.SampleServicePb:1.0@DEFAULT");
        boolean result = meshApiClient.unSubscribeService(request);
        Assert.assertTrue(result);
    }

    @After
    public void after() {
        meshApiClient = null;
        httpMockServer.stop();
    }
}