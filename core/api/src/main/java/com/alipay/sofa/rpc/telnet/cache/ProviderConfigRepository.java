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
package com.alipay.sofa.rpc.telnet.cache;

import com.alipay.sofa.rpc.config.ProviderConfig;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderConfigRepository {
    public static ArrayList<ProviderConfig> providedServiceList = new ArrayList<>();
    private final ConcurrentHashMap<String, ProviderConfig> providedService = new ConcurrentHashMap<>();

    private static volatile ProviderConfigRepository providerConfigRepository;

    private ProviderConfigRepository() {}

    public static ProviderConfigRepository getProviderConfigRepository() {
        if (providerConfigRepository == null){
            synchronized (ProviderConfigRepository.class){
                if (providerConfigRepository == null){
                    providerConfigRepository = new ProviderConfigRepository();
                }
            }
        }
        return providerConfigRepository;
    }

    /**
     * 发布服务
     *
     * @param providerConfig
     */
    public void addProviderConfig(ProviderConfig providerConfig){
        if(providerConfig != null){
            providedService.putIfAbsent(providerConfig.getInterfaceId(),providerConfig);
            providedServiceList.add(providerConfig);
        }
    }


    /**
     * 获取ProviderConfig
     *
     * @param key 唯一id
     * @return the ProviderConfig
     */
    public ProviderConfig getProviderConfig(String key){
        return providedService.get(key);
    }

    /**
     * 获取已发布服务列表
     *
     * @return
     */
    public ArrayList<ProviderConfig> getProvidedServiceList() {
        return providedServiceList;
    }

    /**
     * 获取已发布服务
     *
     * @return 所有ProviderConfig
     */
    public ConcurrentHashMap<String, ProviderConfig> getProvidedServiceMap(){
        return providedService;
    }

    /**
     * 移除 ProviderConfig
     *
     * @param key 唯一id
     */
    public void removeProviderConfig(String key){
        providedService.remove(key);
    }

    /**
     *
     * @param providedServiceList
     */
    public void setProvidedServiceList(ArrayList<ProviderConfig> providedServiceList) {
        this.providedServiceList = providedServiceList;
    }
}

