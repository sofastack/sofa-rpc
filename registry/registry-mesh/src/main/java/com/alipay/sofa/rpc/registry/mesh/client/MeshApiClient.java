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
package com.alipay.sofa.rpc.registry.mesh.client;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.mesh.model.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * @author bystander
 * @version $Id: MeshApiClient.java, v 0.1 2018年03月27日 2:24 PM bystander Exp $
 */
public class MeshApiClient {

    private static final Logger LOGGER         = LoggerFactory.getLogger(MeshApiClient.class);

    private URI                 baseURI;

    /**
     * 连接超时
     */
    private static int          connectTimeout = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT);

    /**
     * 读取超时
     */
    private static int          readTimeout    = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT);

    private static String       errorMessage   = "ERROR";

    private String              host;
    private int                 port;

    public MeshApiClient(String meshAddress) {
        baseURI = URI.create(meshAddress);

        host = baseURI.getHost();
        port = baseURI.getPort();

    }

    public boolean publishService(PublishServiceRequest publishServiceRequest) {

        final String json = JSON.toJSONString(publishServiceRequest);
        String result = httpPost(MeshEndpoint.PUBLISH, json);
        if (!StringUtils.equals(result, errorMessage)) {
            final PublishServiceResult parse = JSON.parseObject(result, PublishServiceResult.class);
            if (parse.isSuccess()) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    public boolean registeApplication(ApplicationInfoRequest applicationInfoRequest) {

        final String json = applicationInfoRequest.toJson();
        String result = httpPost(MeshEndpoint.CONFIGS, json);

        if (!StringUtils.equals(result, errorMessage)) {
            final ApplicationInfoResult parse = JSON.parseObject(result,
                ApplicationInfoResult.class);
            if (parse.isSuccess()) {
                return true;
            }
            return false;
        } else {
            return false;
        }

    }

    public int unPublishService(UnPublishServiceRequest request) {

        final String json = JSON.toJSONString(request);
        String result = httpPost(MeshEndpoint.UN_PUBLISH, json);

        if (!StringUtils.equals(result, errorMessage)) {
            final UnPublishServiceResult parse = JSON.parseObject(result,
                UnPublishServiceResult.class);
            if (parse.isSuccess()) {
                return 1;
            }
            return 0;
        } else {
            return 0;
        }

    }

    public SubscribeServiceResult subscribeService(SubscribeServiceRequest subscribeServiceRequest) {
        final String json = JSON.toJSONString(subscribeServiceRequest);

        String result = httpPost(MeshEndpoint.SUBCRIBE, json);

        SubscribeServiceResult subscribeServiceResult;
        if (!StringUtils.equals(result, errorMessage)) {
            subscribeServiceResult = JSON.parseObject(result, SubscribeServiceResult.class);
            return subscribeServiceResult;
        } else {
            subscribeServiceResult = new SubscribeServiceResult();
            return subscribeServiceResult;
        }
    }

    public boolean unSubscribeService(UnSubscribeServiceRequest request) {
        final String json = JSON.toJSONString(request);

        String result = httpPost(MeshEndpoint.UN_SUBCRIBE, json);

        if (!StringUtils.equals(result, errorMessage)) {
            final UnSubscribeServiceResult parse = JSON.parseObject(result,
                UnSubscribeServiceResult.class);
            if (parse.isSuccess()) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private HttpURLConnection createConnection(URL url, String method, boolean doOutput) {
        HttpURLConnection con;

        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);
            con.setDoOutput(doOutput);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "text/plain");
            return con;
        } catch (IOException e) {
            LOGGER.errorWithApp(null, "uri:" + url, e);
            return null;
        }

    }

    private String readDataFromConnection(HttpURLConnection con) {
        int code = 0;
        URL url;
        String result;
        try {
            code = con.getResponseCode();
            url = con.getURL();
            if (code == 200) {
                // 读取返回内容
                StringBuilder buffer = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),
                    "UTF-8"));
                String temp;
                while ((temp = br.readLine()) != null) {
                    buffer.append(temp);
                    buffer.append("\n");
                }
                result = buffer.toString().trim();
                LOGGER.infoWithApp(null, "uri:" + url + " return result: " + result);
            } else {
                //500等其他错误码,需要异步重新检测的
                LOGGER.infoWithApp(null, "uri:" + url + " return code: " + code);
                result = errorMessage;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return errorMessage;
        }

        return result;
    }

    /**
     * for get method
     *
     * @param path
     * @return
     */
    public String httpGet(String path) {
        HttpURLConnection con = null;
        String result = null;
        try {
            URL url = baseURI.resolve(path).toURL();
            con = createConnection(url, "GET", false);
            con.connect();
            result = readDataFromConnection(con);
        } catch (Exception e) {
            LOGGER.errorWithApp(null, "uri:" + path + " return error: " + e.getMessage());
            result = errorMessage;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    private String httpPost(String path, String json) {
        HttpURLConnection con = null;
        String result = null;
        try {
            URL url = baseURI.resolve(path).toURL();
            con = createConnection(url, "POST", true);
            //  con.connect();

            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            // The URL-encoded contend
            // 正文，正文内容其实跟get的URL中'?'后的参数字符串一致
            // DataOutputStream.writeBytes将字符串中的16位的 unicode字符以8位的字符形式写道流里面
            out.write(json.getBytes("utf-8"));

            out.flush();
            out.close(); // flush and close

            result = readDataFromConnection(con);
        } catch (Exception e) {
            LOGGER.errorWithApp(null, "uri:" + path + " return error: " + e.getMessage());
            result = errorMessage;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
