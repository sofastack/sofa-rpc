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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transmit.TransmitConfig;

import java.util.List;
import java.util.Random;

/**
 * 转发处理器
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitHandler {
    /**
     * LOGGER
     */
    private static final Logger     LOGGER          = LoggerFactory.getLogger(IpTransmitHandler.class);
    /**
     * 应用名
     */
    private String                  appName;
    /**
     * 转发配置
     */
    private TransmitConfig          transmitConfig;
    /**
     * 是否开启转发
     */
    private volatile boolean        transmitStarted = false;
    /**
     * 是否预热中
     */
    private volatile boolean        warmingUp;
    /**
     * 启动时间
     */
    private Long                    startTime;
    /**
     * 地址列表 
     */
    private IpTransmitAddressHolder addressHolder;

    /**
     * 构造函数
     *
     * @param appName        应用名
     * @param transmitConfig 转发配置
     */
    public IpTransmitHandler(String appName, TransmitConfig transmitConfig) {
        this.appName = appName;
        this.transmitConfig = transmitConfig;
        this.addressHolder = new IpTransmitAddressHolder(appName);
    }

    /**
     * 开始转发
     */
    public void startTransmit() {
        // 如果配置了预热转发
        if (transmitConfig.getDuring() > 0) {
            warmingUp = true;
            startTime = System.currentTimeMillis();
            if (StringUtils.isBlank(transmitConfig.getAddress())) {
                // 如果没有配置了预热后继续转发，则在预热结束后销毁
                Thread thread = new Thread(new DestroyRunnable(transmitConfig.getDuring()));
                thread.setDaemon(true);
                thread.setName("STOP-TRANSMIT-" + appName);
                thread.start();
            }
        }
        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "App {} started transmit.", appName);
        }
        transmitStarted = true;
    }

    /**
     * 结束转发
     */
    public void stopTransmit() {
        transmitStarted = false;
        // 销毁应用下全部Client
        IpTransmitClientFactory.destroyByApp(appName);

        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "App {} stopped transmit.", appName);
        }
    }

    /**
     * 是否启动转发
     *
     * @return 启动转发
     */
    public boolean isStarted() {
        return transmitStarted;
    }

    /**
     * 判断是否需要转发
     *
     * @return 判断结果，返回null，表示不需要转发
     */
    public IpTransmitResult judgeRequestTransmitResult() {
        IpTransmitResult result = null;
        if (isWarmingUp()) {
            // 预热中，随机地址
            result = judgeTarget(getRandomUrl(), transmitConfig.getWeightStarting());
        } else {
            // 预热后，指定地址
            result = judgeTarget(transmitConfig.getAddress(), transmitConfig.getWeightStarted());
        }
        return result;
    }

    /**
     * 是否预热中
     *
     * @return 是否预热中
     */
    private boolean isWarmingUp() {
        if (warmingUp) {
            if (RpcRuntimeContext.now() - startTime < transmitConfig.getDuring()) {
                return true;
            } else {
                warmingUp = false;
            }
        }
        return false;
    }

    private IpTransmitResult judgeTarget(String url, double weight) {
        IpTransmitResult result = new IpTransmitResult();
        if (StringUtils.isNotBlank(url) && isTargetTransmit(weight)) {
            result.setTransmit(true);
            result.setTransmitAddress(url);
            result.setTransmitTimeout(transmitConfig.getTransmitTimeout());
        }
        return result;
    }

    private List<String> availableTransmitAddresses;

    private final Random random = new Random();

    private String getRandomUrl() {
        if ((availableTransmitAddresses == null) || (availableTransmitAddresses.isEmpty())) {
            return null;
        }
        int size = availableTransmitAddresses.size();
        if (size == 1) {
            return availableTransmitAddresses.get(0);
        }
        int index = random.nextInt(size);
        return availableTransmitAddresses.get(index);
    }

    private boolean isTargetTransmit(double weight) {
        if (weight == 0) {
            return false;
        } else if (weight == 1) {
            return true;
        } else {
            double b = (Math.random() * 100.0);
            return b < weight * 100;
        }
    }

    public void setAvailableTransmitAddresses(List<String> urls) {
        urls.remove(SystemInfo.getLocalHost());
        this.availableTransmitAddresses = urls;
    }

    public void addAvailableTransmitAddresses(String url) {

        if (!url.equals(SystemInfo.getLocalHost())) {

            this.availableTransmitAddresses.add(url);

        }
    }

    public void deleteAvailableTransmitAddresses(String url) {

        this.availableTransmitAddresses.remove(url);
    }

    public List<String> getAvailableTransmitAddresses() {
        return availableTransmitAddresses;
    }

    /**
     * 当前转发配置
     *
     * @return 转发配置
     */
    public TransmitConfig getTransmitConfig() {
        return transmitConfig;
    }

    /**
     * 得到地址管理器
     * 
     * @return 地址管理器
     */
    public IpTransmitAddressHolder getAddressHolder() {
        return addressHolder;
    }

    /**
     * 销毁线程
     */
    private class DestroyRunnable implements Runnable {

        private long sleepTime = 0;

        DestroyRunnable(long during) {
            this.sleepTime = during;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignore) {
            }
            stopTransmit();
        }
    }

}