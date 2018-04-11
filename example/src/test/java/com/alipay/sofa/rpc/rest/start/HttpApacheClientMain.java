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
package com.alipay.sofa.rpc.rest.start;

import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.rest.ExampleObj;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 *
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class HttpApacheClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpApacheClientMain.class);

    public static void main(String[] args) {
        /*
            注意：windows下服务端若未指定绑定到所有网卡0.0.0.0，则本机客户端是不能直接使用127.0.0.1访问的。
            请查看服务端启动日志，看具体绑定的网卡和端口是哪个：Server have success bind to 10.23.11.22:11090
            例如 http://10.23.11.22:11090
         */
        String url = "http://127.0.0.1:8888/rest/post/1234567890";
        Object[] params = new Object[] { "xxhttpxxx" };
        String result = sendByPost(url, params);
        LOGGER.info("result : {}", result);

        url = "http://127.0.0.1:8888/rest/object";
        ExampleObj example = new ExampleObj();
        example.setId(100);
        example.setName("namename");
        params = new Object[] { example };
        result = sendByPost(url, params);
        ExampleObj objresult = JSON.parseObject(result, ExampleObj.class);
        LOGGER.info("obj result : {}", objresult);

    }

    private static String sendByPost(String url, Object[] params) {
        String response = "";
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httpost = new HttpPost(url);

            httpost.setHeader("token", "1qaz2wsx"); // 服务端需要token
            String json = JSON.toJSONString(params[0]);
            StringEntity entity = new StringEntity(json, Charset.forName("UTF-8"));
            entity.setContentType("application/json");
            httpost.setEntity(entity);
            HttpResponse httpResponse = null;

            httpResponse = httpclient.execute(httpost);
            HttpEntity responseEntity = httpResponse.getEntity();
            LOGGER.info("response status: " + httpResponse.getStatusLine());
            response = EntityUtils.toString(responseEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}