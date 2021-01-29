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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.rpc.base.Sortable;
import com.alipay.sofa.rpc.common.struct.OrderedComparator;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sofa RPC 与配置相关的工具类，不依赖于 Sofa 框架的配置 <br>
 * <p>
 * 大部分参数可配置，优先级：System.setProperty() > 外部加载器(例如可能每个应用独立的sofa-config.properties） > rpc-config.propertirs
 * </p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Deprecated
public final class SofaConfigs {
    /**
     * 外部加载器
     */
    private static final List<ExternalConfigLoader> CONFIG_LOADERS = new ArrayList<ExternalConfigLoader>();
    /**
     * loader变化的锁
     */
    private static ReentrantReadWriteLock           lock           = new ReentrantReadWriteLock();
    /**
     * 读锁，允许并发读 
     */
    private static Lock                             rLock          = lock.readLock();
    /**
     * 写锁，写的时候不允许读 
     */
    private static Lock                             wLock          = lock.writeLock();

    /**
     * rpc-config.properties
     */
    private static Properties                       config;

    /**
     * 初始化 config/rpc-config.properties
     * 初始化失败时，直接报错
     *
     * @return 配置内容
     */
    public static synchronized Properties getConfig() {
        if (config == null) {
            try {
                String rpcConfig = "config/rpc-config.properties";
                InputStream ins = SofaConfigs.class.getClassLoader().getResourceAsStream(rpcConfig);
                if (ins == null) {
                    ins = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(rpcConfig);
                }

                config = new Properties();
                config.load(ins);
            } catch (Exception e) {
                config = new Properties();
            }
        }

        return config;
    }

    /**
     * 解析数字型配置
     *
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static int getIntegerValue(String key, int defaultValue) {
        return getIntegerValue(null, key, defaultValue);
    }

    /**
     * 解析数字型配置
     *
     * @param appName      应用名
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static int getIntegerValue(String appName, String key, int defaultValue) {
        String ret = getStringValue0(appName, key);
        return StringUtils.isEmpty(ret) ? defaultValue : CommonUtils.parseInt(ret, defaultValue);
    }

    /**
     * 获取Boolean格式的Config
     *
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static boolean getBooleanValue(String key, boolean defaultValue) {
        return getBooleanValue(null, key, defaultValue);
    }

    /**
     * 获取Boolean格式的Config
     *
     * @param appName      应用名
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static boolean getBooleanValue(String appName, String key, boolean defaultValue) {
        String ret = getStringValue0(appName, key);
        return StringUtils.isEmpty(ret) ? defaultValue : CommonUtils.parseBoolean(ret, defaultValue);
    }

    /**
     * 通用 获取方法
     * <p>
     * 与没有 appName 的方法相比，该方法不需要传入 appName
     * <p>
     *
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static String getStringValue(String key, String defaultValue) {
        return getStringValue(null, key, defaultValue);
    }

    /**
     * 获取配置值
     *
     * @param appName      应用名
     * @param key          配置项
     * @param defaultValue 默认值
     * @return 配置
     */
    public static String getStringValue(String appName, String key, String defaultValue) {
        String ret = getStringValue0(appName, key);
        return StringUtils.isEmpty(ret) ? defaultValue : ret.trim();
    }

    /**
     * System.getProperty() > 外部配置 > rpc-config.properties
     *
     * @param appName 应用名
     * @param key     配置项
     * @return 配置
     */
    private static String getStringValue0(String appName, String key) {
        String ret = System.getProperty(key);
        if (StringUtils.isNotEmpty(ret)) {
            return ret;
        }
        rLock.lock();
        try {
            for (ExternalConfigLoader configLoader : CONFIG_LOADERS) {
                ret = appName == null ? configLoader.getValue(key)
                    : configLoader.getValue(appName, key);
                if (StringUtils.isNotEmpty(ret)) {
                    return ret;
                }
            }
        } finally {
            rLock.unlock();
        }
        return getConfig().getProperty(key);
    }

    /**
     * 注册外部配置加载器
     *
     * @param configLoader 配置加载器
     */
    public static void registerExternalConfigLoader(ExternalConfigLoader configLoader) {
        wLock.lock();
        try {
            CONFIG_LOADERS.add(configLoader);
            Collections.sort(CONFIG_LOADERS, new OrderedComparator<ExternalConfigLoader>());
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 反注册外部配置加载器
     *
     * @param configLoader 配置加载器
     */
    public static void unRegisterExternalConfigLoader(ExternalConfigLoader configLoader) {
        wLock.lock();
        try {
            CONFIG_LOADERS.remove(configLoader);
            Collections.sort(CONFIG_LOADERS, new OrderedComparator<ExternalConfigLoader>());
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 外部配置加载器
     */
    public static abstract class ExternalConfigLoader implements Sortable {

        /**
         * 顺序
         *
         * @return 顺序，从小到大执行
         */
        @Override
        public int getOrder() {
            return 0;
        }

        /**
         * 获取配置
         *
         * @param key 键
         * @return 值
         */
        public abstract String getValue(String key);

        /**
         * 按应用获取配置
         *
         * @param appName 应用名
         * @param key     键
         * @return 值
         */
        public abstract String getValue(String appName, String key);
    }
}