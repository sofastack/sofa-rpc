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
package com.alipay.sofa.rpc.transmit.ip;

import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistry;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistryCallback;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MockIpTransmitRegistry implements TransmitRegistry {

    public ConcurrentHashMap<String, List<String>>                   DATAS = new ConcurrentHashMap<String, List<String>>();

    public ConcurrentHashMap<String, List<TransmitRegistryCallback>> SUBS  = new ConcurrentHashMap<String, List<TransmitRegistryCallback>>();

    @Override
    public void init(RegistryConfig registryConfig) {

    }

    @Override
    public void register(String appName, String dataId) {
        String key = buildKey(appName, dataId);
        List<String> data = DATAS.get(key);
        if (data == null) {
            data = CommonUtils.putToConcurrentMap(DATAS, key, new CopyOnWriteArrayList<String>());
        }
        String p = SystemInfo.getLocalHost();
        if (data.contains(p)) {
            throw new RuntimeException("Exist provider");
        }
        data.add(p);
        List<TransmitRegistryCallback> sub = SUBS.get(key);
        if (CommonUtils.isNotEmpty(sub)) {
            for (TransmitRegistryCallback callback : sub) {
                callback.handleData(dataId, data);
            }
        }
    }

    private String buildKey(String appName, String dataId) {
        return appName + dataId;
    }

    @Override
    public void subscribe(String appName, String dataId, TransmitRegistryCallback callback) {
        String key = buildKey(appName, dataId);
        List<String> data = DATAS.get(key);
        if (data == null) {
            data = CommonUtils.putToConcurrentMap(DATAS, key, new CopyOnWriteArrayList<String>());
        }
        List<TransmitRegistryCallback> sub = SUBS.get(key);
        if (sub == null) {
            sub = CommonUtils.putToConcurrentMap(SUBS, key, new CopyOnWriteArrayList<TransmitRegistryCallback>());
        }
        sub.add(callback);
        callback.handleData(dataId, data);
    }

    @Override
    public void destroy() {

    }
}
