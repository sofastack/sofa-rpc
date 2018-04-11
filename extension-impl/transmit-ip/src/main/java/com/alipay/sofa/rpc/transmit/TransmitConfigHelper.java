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
package com.alipay.sofa.rpc.transmit;

import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.HashMap;

/**
 * For parse transmit config.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TransmitConfigHelper {

    /**
     * Logger for TransmitConfigHelper
     **/
    private static final Logger LOGGER         = LoggerFactory.getLogger(TransmitConfigHelper.class);

    // 配置变量
    private static final String ADDRESS        = "address";
    private static final String WEIGHTSTARTING = "weightStarting";
    private static final String WEIGHTSTARTED  = "weightStarted";
    private static final String DURING         = "during";
    private static final String UNIQUEID       = "uniqueId";
    private static final String COLON          = ":";
    private static final String COMMA          = ",";

    /**
     * 解析配置。
     * <p>
     * 参考配置 :<pre><code>
     *     core_proxy_url=weightStarting:1,during:3,weightStarted:0,address:127.0.0.1,uniqueId:core_unique
     *     core_unique = xxx
     * </code></pre>
     * <p>
     * 配置参考：
     * <li> weightStarting: 预热期内的转发权重或概率，RPC 框架内部会在集群中随机找一台机器以此权重转出。 【1是一直转发 0是不转发】</il>
     * <li>during: 预热期的时间长度，单位为秒</il>
     * <li>weightStarted: 预热期过后的转发权重，将会一直生效   【1是一直转发 0是不转发】</il>
     * <li>address: 预热期过后的转发地址，将会一直生效</il>
     * <li>uniqueId: 同 appName 多集群部署的情况下，要区别不同集群可以通过配置此项区分。指定一个自定义的系统变量，保证集群唯一即可。
     * core_unique 是一个 sofa-config.properties 的配置，可以动态替换; 使用方式是在sofa-config.properties中类似定义 core_unique=xxx
     * 【uniqueId 不是 service或者reference 的 uniqueId】</il>
     * </p>
     *
     * @param appName           应用名
     * @param transmitConfigStr 配置字符串
     * @return 配置对象
     */
    public static TransmitConfig parseTransmitConfig(String appName, String transmitConfigStr) {
        TransmitConfig config = new TransmitConfig();
        if (StringUtils.isEmpty(transmitConfigStr)) {
            return config;
        }
        transmitConfigStr = transmitConfigStr.trim();
        if (isRegularConfig(transmitConfigStr)) {
            // core_proxy_url=weightStarting:0.7,during:120,weightStarted:0.2,address:x.x.x.x,uniqueId:core_unique
            try {
                HashMap<String, String> proxyConfigs = parseTransmitConfig(transmitConfigStr);
                //uniqueId
                String uniqueIdConfig = proxyConfigs.get(UNIQUEID);
                if (StringUtils.isNotBlank(uniqueIdConfig)) {
                    String uniqueIdValue = SofaConfigs.getStringValue(uniqueIdConfig, uniqueIdConfig);
                    config.setUniqueIdValue(uniqueIdValue);
                }
                //transmitUrl
                String addressConfig = proxyConfigs.get(ADDRESS);
                if (StringUtils.isNotBlank(addressConfig)) {
                    config.setAddress(addressConfig);
                }
                String duringConfig = proxyConfigs.get(DURING);
                if (StringUtils.isNotBlank(duringConfig) && !"0".equals(duringConfig.trim())) {
                    // 解析预热时间+预热权重+预热后权重 （秒转为毫秒）
                    config.setDuring(Long.parseLong(duringConfig.trim()) * 1000);
                    String weightStartingConfig = proxyConfigs.get(WEIGHTSTARTING);
                    if (StringUtils.isNotBlank(weightStartingConfig)) {
                        config.setWeightStarting(Double.parseDouble(weightStartingConfig.trim()));
                    }
                    String weightStartedConfig = proxyConfigs.get(WEIGHTSTARTED);
                    if (StringUtils.isNotBlank(weightStartedConfig)) {
                        config.setWeightStarted(Double.parseDouble(weightStartedConfig.trim()));
                    }
                } else {
                    // 只解析预热后权重
                    String weightStartedConfig = proxyConfigs.get(WEIGHTSTARTED);
                    if (StringUtils.isNotBlank(weightStartedConfig)) {
                        config.setWeightStarted(Double.parseDouble(weightStartedConfig.trim()));
                    }
                }
                return config;
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.ERROR_TRANSMIT_PARSE));
                return new TransmitConfig();
            }
        } else {
            // core_proxy_url=x.x.x.x
            config.setAddress(transmitConfigStr);
            config.setWeightStarted(1.0d);
            return config;
        }
    }

    private static boolean isRegularConfig(String proxyConfig) {
        return proxyConfig.contains(COLON) || proxyConfig.endsWith(COMMA);
    }

    private static HashMap<String, String> parseTransmitConfig(String transmitConfig) {
        HashMap<String, String> transmitConfigs = new HashMap<String, String>();
        String[] items = transmitConfig.split(",");
        for (String item : items) {
            if (StringUtils.isNotBlank(item)) {
                String[] kv = item.split(":");
                if (kv.length > 1 && StringUtils.isNotEmpty(kv[1].trim())) {
                    transmitConfigs.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return transmitConfigs;
    }
}
