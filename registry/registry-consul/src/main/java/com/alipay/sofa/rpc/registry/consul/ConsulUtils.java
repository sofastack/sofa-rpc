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
package com.alipay.sofa.rpc.registry.consul;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.buildUniqueName;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.getServerHost;

/**
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class ConsulUtils {

    private static final Pattern META_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9\\-_]+");

    /**
     * Key can only contain A-Z a-z 0-9 _ and -.
     * @param key
     * @return
     */
    public static boolean isValidMetaKey(String key) {
        return META_KEY_PATTERN.matcher(key).matches();
    }

    public static String buildServiceName(AbstractInterfaceConfig config) {
        String consulServiceName = config.getParameter(ConsulConstants.CONSUL_SERVICE_NAME_KEY);
        if (consulServiceName != null) {
            return consulServiceName;
        }
        return config.getInterfaceId();
    }

    public static List<String> buildServiceIds(ProviderConfig<?> config) {
        List<ServerConfig> servers = config.getServer();
        if (CommonUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }
        return servers.stream()
                .map(server -> buildServiceId(config, server))
                .collect(Collectors.toList());
    }

    public static String buildServiceId(ProviderConfig config, ServerConfig server) {
        return String.join("-", buildUniqueName(config, server.getProtocol()), getServerHost(server),
            String.valueOf(server.getPort()));
    }
}
