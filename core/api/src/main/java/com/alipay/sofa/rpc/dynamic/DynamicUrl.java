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
package com.alipay.sofa.rpc.dynamic;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Narziss
 * @version DynamicUrl.java, v 0.1 2024年10月28日 21:23 Narziss
 */
public class DynamicUrl {

    private final String originalUrl;
    private final String protocol;
    private final String address;
    private final String host;
    private final int port;
    private final String path;
    private final Map<String, String> params = new HashMap<>();

    /**
     * @param configCenterAddress example: apollo://127.0.0.1:8080/config?appId=xxx&cluster=yyy
     */
    public DynamicUrl(String configCenterAddress) {
        this.originalUrl = configCenterAddress;
        // 正则表达式解析协议、主机、端口、路径和参数，其中路径和参数是可选的
        String regex = "^(\\w+)://([^:/]+):(\\d+)(/[^?]*)?(\\?.*)?$";
        Matcher matcher = Pattern.compile(regex).matcher(configCenterAddress);
        if (matcher.find()) {
            this.protocol = matcher.group(1);
            this.host = matcher.group(2);
            this.port = Integer.parseInt(matcher.group(3));
            // 判断路径是否为空或者为 "/"
            this.path = (matcher.group(4) != null && !matcher.group(4).equals("/")) ? matcher.group(4) : "";
            this.address = this.host + ":" + this.port + this.path;
            if (matcher.group(5) != null) {
                parseQueryParams(matcher.group(5).substring(1));
            }
        } else {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }

    private void parseQueryParams(String query) {
        String[] paramPairs = query.split("&");
        for (String paramPair : paramPairs) {
            String[] keyValue = paramPair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getParam(String key) {
        return params.get(key);
    }
}
