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

/**
 * Config of transmit.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TransmitConfig {

    /**
     * 预热期间转发比例（0代表不转发，1代表全部转发），例如0.5代表50%转发
     */
    private double weightStarting  = 1.0d;

    /**
     * 预热后转发比例（0代表不转发，1代表全部转发），例如0.5代表50%转发
     */
    private double weightStarted   = 0.0d;

    /**
     * 预热时间，单位毫秒
     */
    private long   during          = 0;

    /**
     * 预热后转发地址
     */
    private String address;

    /**
     * 区别同应用的不同集群(core_unique)
     */
    private String uniqueIdValue;

    /**
     * 转发超时时间，单位ms。默认10000，rpc_transmit_url_timeout_tr
     */
    private int    transmitTimeout = 10000;

    /**
     * Gets weight starting.
     *
     * @return the weight starting
     */
    public double getWeightStarting() {
        return weightStarting;
    }

    /**
     * Sets weight starting.
     *
     * @param weightStarting the weight starting
     * @return the weight starting
     */
    public TransmitConfig setWeightStarting(double weightStarting) {
        this.weightStarting = weightStarting;
        return this;
    }

    /**
     * Gets weight started.
     *
     * @return the weight started
     */
    public double getWeightStarted() {
        return weightStarted;
    }

    /**
     * Sets weight started.
     *
     * @param weightStarted the weight started
     * @return the weight started
     */
    public TransmitConfig setWeightStarted(double weightStarted) {
        this.weightStarted = weightStarted;
        return this;
    }

    /**
     * Gets during.
     *
     * @return the during
     */
    public long getDuring() {
        return during;
    }

    /**
     * Sets during.
     *
     * @param during the during
     * @return the during
     */
    public TransmitConfig setDuring(long during) {
        this.during = during;
        return this;
    }

    /**
     * Sets transmit timeout.
     *
     * @param transmitTimeout the transmit timeout
     * @return the transmit timeout
     */
    public TransmitConfig setTransmitTimeout(int transmitTimeout) {
        this.transmitTimeout = transmitTimeout;
        return this;
    }

    /**
     * Gets unique id value.
     *
     * @return the unique id value
     */
    public String getUniqueIdValue() {
        return uniqueIdValue;
    }

    /**
     * Sets unique id value.
     *
     * @param uniqueIdValue the unique id value
     * @return the unique id value
     */
    public TransmitConfig setUniqueIdValue(String uniqueIdValue) {
        this.uniqueIdValue = uniqueIdValue;
        return this;
    }

    /**
     * Gets transmit timeout.
     *
     * @return the transmit timeout
     */
    public Integer getTransmitTimeout() {
        return transmitTimeout;
    }

    /**
     * Sets transmit timeout.
     *
     * @param transmitTimeout the transmit timeout
     * @return the transmit timeout
     */
    public TransmitConfig setTransmitTimeout(Integer transmitTimeout) {
        this.transmitTimeout = transmitTimeout;
        return this;
    }

    /**
     * Gets address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets address.
     *
     * @param address the address
     * @return the address
     */
    public TransmitConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public String toString() {
        return "TransmitConfig{" +
            "weightStarting=" + weightStarting +
            ", weightStarted=" + weightStarted +
            ", during=" + during +
            ", address='" + address + '\'' +
            ", uniqueIdValue='" + uniqueIdValue + '\'' +
            ", transmitTimeout=" + transmitTimeout +
            '}';
    }
}
