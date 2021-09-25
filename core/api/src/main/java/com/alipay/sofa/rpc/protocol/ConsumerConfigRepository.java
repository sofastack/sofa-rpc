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
package com.alipay.sofa.rpc.protocol;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerConfigRepository {
    private static volatile ConsumerConfigRepository consumerConfigRepository;
    public static ArrayList<ConsumerConfig> referredServiceList = new ArrayList<>();
    public static ConcurrentHashMap<String, ConsumerConfig> referredService = new ConcurrentHashMap<>();

    private ConsumerConfigRepository() {
    }

    public static ConsumerConfigRepository getConsumerConfigRepository() {
        if (consumerConfigRepository == null) {
            synchronized (ConsumerConfigRepository.class) {
                if (consumerConfigRepository == null) {
                    consumerConfigRepository = new ConsumerConfigRepository();
                }
            }
        }
        return consumerConfigRepository;
    }

    /**
     * 引用服务
     *
     * @param consumerConfig
     */
    public void addConsumerConfig(ConsumerConfig consumerConfig) {
        if (consumerConfig != null) {
            referredService.putIfAbsent(consumerConfig.getInterfaceId(), consumerConfig);
            referredServiceList.add(consumerConfig);
        }
    }


    /**
     * 获取引用服务
     *
     * @param key
     * @return
     */
    public ConsumerConfig getConsumerConfig(String key) {
        return referredService.get(key);
    }

    /**
     * 移除引用服务
     *
     * @param key
     */
    public void removeConsumerConfig(String key) {
        referredService.remove(key);
    }

    /**
     * 获取引用服务列表
     *
     * @return
     */
    public ArrayList<ConsumerConfig> getReferredServiceList() {
        return referredServiceList;
    }

    /**
     * 获取引用服务列表
     *
     * @return
     */
    public ConcurrentHashMap<String, ConsumerConfig> getReferredService() {
        return referredService;
    }

    /**
     * @param referredServiceList
     */
    public void setReferredServiceList(ArrayList<ConsumerConfig> referredServiceList) {
        ConsumerConfigRepository.referredServiceList = this.referredServiceList;
    }
}
