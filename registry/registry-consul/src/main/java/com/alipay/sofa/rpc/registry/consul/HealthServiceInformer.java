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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.convertInstanceToUrl;

/**
 * Observe the providers from consul and notify the consumers
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class HealthServiceInformer {

    private static final Logger           LOGGER = LoggerFactory
                                                     .getLogger(HealthServiceInformer.class);

    private String                        serviceName;

    private String                        tag;

    private Response<List<HealthService>> currentData;

    private ConsulClient                  consulClient;

    private ConsulRegistryProperties      properties;

    private List<ProviderInfoListener>    listeners;

    private ScheduledExecutorService      watchExecutor;

    public HealthServiceInformer(String serviceName, String tag, ConsulClient consulClient, ConsulRegistryProperties properties) {
        this.serviceName = serviceName;
        this.tag = tag;
        this.consulClient = consulClient;
        this.properties = properties;
        this.listeners = new ArrayList<>();
    }

    private void watchHealthService() {
        try {
            HealthServicesRequest request = HealthServicesRequest.newBuilder()
                    .setTag(tag)
                    .setQueryParams(new QueryParams(properties.getWatchTimeout(), currentData.getConsulIndex()))
                    .setPassing(true)
                    .build();
            Response<List<HealthService>> response = consulClient.getHealthServices(serviceName, request);
            if (response.getConsulIndex().equals(currentData.getConsulIndex())) {
                return;
            }
            this.currentData = response;
            ProviderGroup providerGroup = new ProviderGroup(currentProviders());
            listeners.stream().filter(Objects::nonNull).forEach(l -> l.updateProviders(providerGroup));
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_WATCH_HEALTH ,"Consul"), e);
        }
    }

    public void init() {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(tag)
                .setQueryParams(QueryParams.DEFAULT)
                .setPassing(true)
                .build();
        this.currentData = consulClient.getHealthServices(serviceName, request);

        this.watchExecutor = Executors.newSingleThreadScheduledExecutor();
        this.watchExecutor.scheduleWithFixedDelay(this::watchHealthService, properties.getLookupInterval(), properties.getLookupInterval(), TimeUnit.MILLISECONDS);
    }

    public List<ProviderInfo> currentProviders() {
        return currentData.getValue().stream()
                .map(HealthService::getService)
                .map(service -> ProviderHelper.toProviderInfo(convertInstanceToUrl(service.getAddress(), service.getPort(), service.getMeta())))
                .collect(Collectors.toList());
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
