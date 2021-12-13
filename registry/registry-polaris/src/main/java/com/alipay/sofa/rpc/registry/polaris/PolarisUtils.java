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
package com.alipay.sofa.rpc.registry.polaris;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import com.alipay.sofa.rpc.server.Server;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.convertProviderToMap;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.getServerHost;

public class PolarisUtils {

    static public List<InstanceDeregisterRequest> buildPolarisDeregister(ProviderConfig config) {

        List<ServerConfig> servers = config.getServer();
        if (CommonUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }

        List<InstanceDeregisterRequest> res = new ArrayList<>();
        for (ServerConfig server : servers) {
            InstanceDeregisterRequest service = new InstanceDeregisterRequest();
            service.setNamespace(config.getAppName());
            service.setService(config.getInterfaceId());
            service.setHost(getServerHost(server));
            service.setPort(server.getPort());
            service.setTimeoutMs(config.getTimeout());
            res.add(service);
        }

        return res;
    }

    static public GetAllInstancesRequest buildPolarisGetAllInstancesRequest(ConsumerConfig config) {
        return null;
    }

}
