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
package com.alipay.sofa.rpc.client.http;

import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 *  @author <a href=mailto:zhiyuan.lzy@antfin.com>zhiyuan.lzy</a>
 */
public class RpcHttpClient {
    private static final Logger          LOGGER   = LoggerFactory.getLogger(RpcHttpClient.class);

    private volatile CloseableHttpClient closeableHttpClient;

    private static RpcHttpClient         INSTANCE = new RpcHttpClient();

    private RpcHttpClient() {
        shutdownHttpClientHook();
    }

    public static RpcHttpClient getInstance() {
        return INSTANCE;
    }

    public <T> T doGet(String url, Class<T> tClass) throws Throwable {
        long start = System.currentTimeMillis();
        CloseableHttpClient httpClient = getCloseableHttpClient();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            RequestConfig requestConfig = parseRequestConfig();
            httpGet.setConfig(requestConfig);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("http client get success. url: {}. latency: {}ms.", url, System.currentTimeMillis() -
                    start);
            }
            return JSON.parseObject(EntityUtils.toString(entity), tClass);
        } catch (Throwable throwable) {
            LOGGER.error("http client get error. url: " + url + ". latency: " + (System.currentTimeMillis() - start) +
                "ms.", throwable);
            throw throwable;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("http client get close response error.", e);
                }
            }
        }
    }

    public <T> T doPost(String url, String jsonBody, Class<T> tClass) throws Throwable {
        long start = System.currentTimeMillis();
        CloseableHttpClient httpClient = getCloseableHttpClient();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            RequestConfig requestConfig = parseRequestConfig();
            httpPost.setConfig(requestConfig);

            StringEntity requestEntity = new StringEntity(jsonBody, "utf-8");
            requestEntity.setContentEncoding("UTF-8");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(requestEntity);

            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("http client post success. url: {}. latency: {}ms.", url, System.currentTimeMillis() -
                    start);
            }
            return JSON.parseObject(EntityUtils.toString(entity), tClass);
        } catch (Throwable throwable) {
            LOGGER.error(
                "http client post error. url: " + url + ", body: " + jsonBody + ". latency: " +
                    (System.currentTimeMillis() - start)
                    + "ms.", throwable);
            throw throwable;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("http client post close response error.", e);
                }
            }
        }
    }

    private CloseableHttpClient getCloseableHttpClient() {
        if (closeableHttpClient == null) {
            synchronized (this) {
                if (closeableHttpClient == null) {
                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                    closeableHttpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
                }
            }
        }
        return closeableHttpClient;
    }

    private RequestConfig parseRequestConfig() {
        return RequestConfig.custom().build();
    }

    private void shutdownHttpClientHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    if (closeableHttpClient != null) {
                        closeableHttpClient.close();
                    }
                } catch (IOException e) {
                    LOGGER.warn("http client close closeableHttpClient error when shutdown jvm.", e);
                }
            }

        });
    }
}