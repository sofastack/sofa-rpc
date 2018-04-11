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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RestValidationTest extends BaseRestTest {

    @Test
    public void testMax() throws IOException, InterruptedException {

        //rpc调用
        Assert.assertEquals("666", restService.validationMax(666));
        try {
            restService.validationMax(588L);
        } catch (Exception e) {
            Assert.assertEquals("Send message to remote catch error: HTTP 400 Bad Request", e.getMessage());
        }

        //http客户端调用
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8803/rest/validationMax/605");
        HttpResponse httpResponse = httpclient.execute(httpGet);
        String result = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertEquals("605", result);

        DefaultHttpClient httpclient2 = new DefaultHttpClient();
        HttpGet httpGet2 = new HttpGet("http://127.0.0.1:8803/rest/validationMax/599");
        HttpResponse httpResponse2 = httpclient2.execute(httpGet2);
        String result2 = EntityUtils.toString(httpResponse2.getEntity());
        Assert.assertTrue(!"599".equals(result2));

    }

}