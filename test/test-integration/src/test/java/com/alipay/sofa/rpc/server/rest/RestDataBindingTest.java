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

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RestDataBindingTest extends BaseRestTest {

    @Test
    public void testHeader() throws IOException, InterruptedException {

        //rpc调用
        Assert.assertEquals("header", restService.bindHeader("header"));

        //http客户端调用
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httpost = new HttpPost("http://127.0.0.1:8803/rest/bindHeader");
        httpost.setHeader("headerP", "headerBBB");

        HttpResponse httpResponse = httpclient.execute(httpost);
        String result = EntityUtils.toString(httpResponse.getEntity());

        Assert.assertEquals("headerBBB", result);
    }

    @Test
    public void testQuery() throws IOException {
        Assert.assertEquals("query", restService.bindQuery("query"));

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8803/rest/bindQuery?queryP=queryBBB");

        HttpResponse httpResponse = httpclient.execute(httpGet);
        String result = EntityUtils.toString(httpResponse.getEntity());

        Assert.assertEquals("queryBBB", result);
    }

    @Test
    public void testForm() throws IOException {
        Assert.assertEquals("form", restService.bindForm("form"));

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:8803/rest/bindForm");

        List<NameValuePair> list = new ArrayList<NameValuePair>();
        list.add(new BasicNameValuePair("formP", "formBBB"));
        HttpEntity httpEntity = new UrlEncodedFormEntity(list, Consts.UTF_8);

        httpPost.setEntity(httpEntity);

        HttpResponse httpResponse = httpclient.execute(httpPost);
        String result = EntityUtils.toString(httpResponse.getEntity());

        Assert.assertEquals("formBBB", result);
    }

}