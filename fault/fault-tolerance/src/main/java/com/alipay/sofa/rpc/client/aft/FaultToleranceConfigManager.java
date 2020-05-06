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
package com.alipay.sofa.rpc.client.aft;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Fault tolerance config manager.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class FaultToleranceConfigManager {

    /**
     * Logger for this class
     */
    private static final Logger                                      LOGGER      = LoggerFactory
                                                                                     .getLogger(FaultToleranceConfigManager.class);

    /**
     * All fault-tolerance config of apps
     */
    private static final ConcurrentMap<String, FaultToleranceConfig> APP_CONFIGS = new ConcurrentHashMap<String, FaultToleranceConfig>();

    /**
     * Default fault-tolerance config
     */
    private static final FaultToleranceConfig                        DEFAULT_CFG = new FaultToleranceConfig();

    /**
     * 
     */
    private static volatile boolean                                  aftEnable   = false;

    /**
     * Put app config.
     *
     * @param appName the app name
     * @param value   the value
     */
    public static void putAppConfig(String appName, FaultToleranceConfig value) {
        if (appName == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("App name is null when put fault-tolerance config");
            }
            return;
        }
        if (value != null) {
            APP_CONFIGS.put(appName, value);
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, "Get a new resource, value[" + value + "]");
            }
        } else {
            APP_CONFIGS.remove(appName);
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, "Remove a resource, key[" + appName + "]");
            }
        }
        calcEnable();
    }

    static void calcEnable() {
        for (FaultToleranceConfig config : APP_CONFIGS.values()) {
            if (config.isRegulationEffective()) {
                aftEnable = true;
                return;
            }
        }
        aftEnable = false;
    }

    /**
     * If one app enable this AFT, return true.
     *
     * @return is AFT enable.
     */
    public static boolean isEnable() {
        return aftEnable;
    }

    /**
     * Get config if absent, else return default
     *
     * @param appName App name
     * @return FaultToleranceConfig of this app, or default config
     */
    public static FaultToleranceConfig getConfig(String appName) {
        if (appName == null) {
            return DEFAULT_CFG;
        } else {
            FaultToleranceConfig config = APP_CONFIGS.get(appName);
            return config == null ? DEFAULT_CFG : config;
        }
    }

    /**
     * getTimeWindow
     *
     * @param appName App name
     * @return time window
     */
    public static long getTimeWindow(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getTimeWindow();
    }

    /**
     * Gets least invoke count.
     *
     * @param appName the app name
     * @return the least invoke count
     */
    public static long getLeastCallCount(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getLeastCallCount();
    }

    /**
     * Gets least window count.
     *
     * @param appName the app name
     * @return the least window count
     */
    public static long getLeastWindowCount(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getLeastWindowCount();
    }

    /**
     * Gets least window exception rate multiple.
     *
     * @param appName the app name
     * @return the least window exception rate multiple
     */
    public static double getLeastWindowExceptionRateMultiple(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getLeastWindowExceptionRateMultiple();
    }

    /**
     * Gets weight degrade rate.
     *
     * @param appName the app name
     * @return the weight degrade rate
     */
    public static double getWeightDegradeRate(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getWeightDegradeRate();
    }

    /**
     * Is regulation effective boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    public static boolean isRegulationEffective(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.isRegulationEffective();
    }

    /**
     * Is degrade effective boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    public static boolean isDegradeEffective(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.isDegradeEffective();
    }

    /**
     * Gets degrade least weight.
     *
     * @param appName the app name
     * @return the degrade least weight
     */
    public static int getDegradeLeastWeight(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getDegradeLeastWeight();
    }

    /**
     * Gets weight recover rate.
     *
     * @param appName the app name
     * @return the weight recover rate
     */
    public static double getWeightRecoverRate(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getWeightRecoverRate();
    }

    /**
     * Gets degrade max ip count.
     *
     * @param appName the app name
     * @return the degrade max ip count
     */
    public static int getDegradeMaxIpCount(String appName) {
        FaultToleranceConfig config = getConfig(appName);
        return config.getDegradeMaxIpCount();
    }
}
