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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.convertInstanceToUrl;

/**
 * observe the providers from polaris and notify the consumers
 *
 * @author <a href=mailto:bner666@gmail.com>ZhangLibin</a>
 */
public class PolarisWatcher {
    private static final Logger        LOGGER = LoggerFactory.getLogger(PolarisWatcher.class);

    private String                     nameSpace;
    private String                     serviceName;
    private String                     protocol;

    private InstancesResponse          currentData;

    private ConsumerAPI                consumerAPI;

    private PolarisRegistryProperties  properties;

    private List<ProviderInfoListener> listeners;

    private ScheduledExecutorService   watchExecutor;

    public PolarisWatcher(String nameSpace, String serviceName, String protocol, ConsumerAPI consumerAPI, PolarisRegistryProperties properties) {
        this.nameSpace = nameSpace;
        this.serviceName = serviceName;
        this.protocol = protocol;
        this.consumerAPI = consumerAPI;
        this.properties = properties;
        this.listeners = new ArrayList<>();
    }

    private void watchService() {
        try {
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setNamespace(nameSpace);
            getAllInstancesRequest.setService(serviceName);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("protocol", protocol);
            getAllInstancesRequest.setMetadata(parameters);
            InstancesResponse response = consumerAPI.getAllInstance(getAllInstancesRequest);
            this.currentData = response;
            ProviderGroup providerGroup = new ProviderGroup(currentProviders());
            listeners.stream().filter(Objects::nonNull).forEach(l -> l.updateProviders(providerGroup));
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_WATCH_HEALTH, "Polaris"), e);
        }
    }

    public List<ProviderInfo> currentProviders() {
        List<ProviderInfo> providerInfos = new ArrayList<>();
        Instance[] instances = currentData.getInstances();
        for (Instance instance : instances) {
            ProviderInfo providerInfo = ProviderHelper.toProviderInfo(convertInstanceToUrl(instance.getHost(), instance.getPort(), currentData.getMetadata()));
            providerInfos.add(providerInfo);
        }
        return providerInfos;
    }

    public void init() {
        GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
        getAllInstancesRequest.setNamespace(nameSpace);
        getAllInstancesRequest.setService(serviceName);
        this.currentData = consumerAPI.getAllInstance(getAllInstancesRequest);
        this.watchExecutor = Executors.newSingleThreadScheduledExecutor();
        this.watchExecutor.scheduleWithFixedDelay(this::watchService, properties.getLookupInterval(), properties.getLookupInterval(), TimeUnit.MILLISECONDS);
    }

    public void addListener(ProviderInfoListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProviderInfoListener listener) {
        listeners.remove(listener);
    }

    public int getListenerSize() {
        return listeners.size();
    }

    public void shutdown() {
        this.watchExecutor.shutdown();
    }
}
