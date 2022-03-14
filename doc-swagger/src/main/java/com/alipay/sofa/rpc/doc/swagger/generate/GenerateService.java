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
package com.alipay.sofa.rpc.doc.swagger.generate;

import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class GenerateService {

    private String defaultProtocol = "bolt";
    private String basePath        = "/rest/";

    public GenerateService() {
    }

    public GenerateService(String defaultProtocol, String basePath) {
        this.defaultProtocol = defaultProtocol;
        this.basePath = basePath;
    }

    public String generate() {
        return generate(defaultProtocol);
    }

    public String generate(String protocol) {
        if (protocol == null) {
            protocol = defaultProtocol;
        }
        Swagger swagger = new Swagger();

        swagger.setInfo(getInfo());
        swagger.setBasePath(basePath);

        Map<Class<?>, Object> interfaceMapRef = new HashMap<>();
        List<ProviderBootstrap> providerBootstraps = RpcRuntimeContext.getProviderConfigs();
        for (ProviderBootstrap providerBootstrap : providerBootstraps) {
            ProviderConfig providerConfig = providerBootstrap.getProviderConfig();
            List<ServerConfig> server = providerConfig.getServer();
            for (ServerConfig serverConfig : server) {
                if (serverConfig.getProtocol().equals(protocol)) {
                    interfaceMapRef.put(providerConfig.getProxyClass(), providerConfig.getRef());
                    break;
                }
            }
        }

        Reader.read(swagger, interfaceMapRef, "");
        String result = null;
        try {
            result = Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Info getInfo() {
        Info info = new Info();
        info.setVersion("");
        info.setTitle("Swagger API");
        return info;
    }
}
