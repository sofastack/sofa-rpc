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

/**
 * The type Fault tolerance config.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class FaultToleranceConfig {

    /**
     * 默认时间窗口大小
     */
    private long    timeWindow                       = 10L;

    /**
     * 即时开始,调用多少次以上才调控,如果不足,不处理.无需调控.
     */
    private long    leastCallCount                   = 100L;

    /**
     * 默认InvocationStat如果要参与统计的窗口内最低调用次数,时间窗口内,至少调用的次数.在时间窗口内总共都不足10,认为不需要调控.
     */
    private long    leastWindowCount                 = 10L;

    /**
     * 当前机器是平均异常率的多少倍才降级.如果某个ip的异常率是该服务所有ip的平均异常率到该比例时则会被判定为异常
     */
    private double  leastWindowExceptionRateMultiple = 6D;

    /**
     * 是否开启调控.
     */
    private boolean regulationEffective              = false;

    /**
     * 默认每次权重降级的比率
     */
    private double  weightDegradeRate                = 0.05D;

    /**
     * 默认是否进行降级
     */
    private boolean degradeEffective                 = false;

    /**
     * 默认权重降级的最小值，如果权重降级到小于该值则设定为该值
     */
    private int     degradeLeastWeight               = 1;

    /**
     * 默认恢复速率.2倍恢复
     */
    private double  weightRecoverRate                = 2;

    /**
     * 默认一个服务能够调控的最大ip数
     */
    private int     degradeMaxIpCount                = 2;

    /**
     * Gets time window.
     *
     * @return the time window
     */
    public long getTimeWindow() {
        return timeWindow;
    }

    /**
     * Sets time window.
     *
     * @param timeWindow the time window
     */
    public void setTimeWindow(long timeWindow) {
        this.timeWindow = timeWindow;
    }

    /**
     * Gets least invoke count.
     *
     * @return the least invoke count
     */
    public long getLeastCallCount() {
        return leastCallCount;
    }

    /**
     * Sets least invoke count.
     *
     * @param leastCallCount the least invoke count
     */
    public void setLeastCallCount(long leastCallCount) {
        this.leastCallCount = leastCallCount;
    }

    /**
     * Gets least window count.
     *
     * @return the least window count
     */
    public long getLeastWindowCount() {
        return leastWindowCount;
    }

    /**
     * Sets least window count.
     *
     * @param leastWindowCount the least window count
     */
    public void setLeastWindowCount(long leastWindowCount) {
        this.leastWindowCount = leastWindowCount;
    }

    /**
     * Gets least window exception rate multiple.
     *
     * @return the least window exception rate multiple
     */
    public double getLeastWindowExceptionRateMultiple() {
        return leastWindowExceptionRateMultiple;
    }

    /**
     * Sets least window exception rate multiple.
     *
     * @param leastWindowExceptionRateMultiple the least window exception rate multiple
     */
    public void setLeastWindowExceptionRateMultiple(double leastWindowExceptionRateMultiple) {
        this.leastWindowExceptionRateMultiple = leastWindowExceptionRateMultiple;
    }

    /**
     * Is regulation effective boolean.
     *
     * @return the boolean
     */
    public boolean isRegulationEffective() {
        return regulationEffective;
    }

    /**
     * Sets regulation effective.
     *
     * @param regulationEffective the regulation effective
     */
    public void setRegulationEffective(boolean regulationEffective) {
        this.regulationEffective = regulationEffective;
        FaultToleranceConfigManager.calcEnable();
    }

    /**
     * Gets weight degrade rate.
     *
     * @return the weight degrade rate
     */
    public double getWeightDegradeRate() {
        return weightDegradeRate;
    }

    /**
     * Sets weight degrade rate.
     *
     * @param weightDegradeRate the weight degrade rate
     */
    public void setWeightDegradeRate(double weightDegradeRate) {
        this.weightDegradeRate = weightDegradeRate;
    }

    /**
     * Is degrade effective boolean.
     *
     * @return the boolean
     */
    public boolean isDegradeEffective() {
        return degradeEffective;
    }

    /**
     * Sets degrade effective.
     *
     * @param degradeEffective the degrade effective
     */
    public void setDegradeEffective(boolean degradeEffective) {
        this.degradeEffective = degradeEffective;
    }

    /**
     * Gets degrade least weight.
     *
     * @return the degrade least weight
     */
    public int getDegradeLeastWeight() {
        return degradeLeastWeight;
    }

    /**
     * Sets degrade least weight.
     *
     * @param degradeLeastWeight the degrade least weight
     */
    public void setDegradeLeastWeight(int degradeLeastWeight) {
        this.degradeLeastWeight = degradeLeastWeight;
    }

    /**
     * Gets weight recover rate.
     *
     * @return the weight recover rate
     */
    public double getWeightRecoverRate() {
        return weightRecoverRate;
    }

    /**
     * Sets weight recover rate.
     *
     * @param weightRecoverRate the weight recover rate
     */
    public void setWeightRecoverRate(double weightRecoverRate) {
        this.weightRecoverRate = weightRecoverRate;
    }

    /**
     * Gets degrade max ip count.
     *
     * @return the degrade max ip count
     */
    public int getDegradeMaxIpCount() {
        return degradeMaxIpCount;
    }

    /**
     * Sets degrade max ip count.
     *
     * @param degradeMaxIpCount the degrade max ip count
     */
    public void setDegradeMaxIpCount(int degradeMaxIpCount) {
        this.degradeMaxIpCount = degradeMaxIpCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FaultToleranceConfig{");
        sb.append("timeWindow=").append(timeWindow);
        sb.append(", leastCallCount=").append(leastCallCount);
        sb.append(", leastWindowCount=").append(leastWindowCount);
        sb.append(", leastWindowExceptionRateMultiple=").append(leastWindowExceptionRateMultiple);
        sb.append(", regulationEffective=").append(regulationEffective);
        sb.append(", weightDegradeRate=").append(weightDegradeRate);
        sb.append(", degradeEffective=").append(degradeEffective);
        sb.append(", degradeLeastWeight=").append(degradeLeastWeight);
        sb.append(", weightRecoverRate=").append(weightRecoverRate);
        sb.append(", degradeMaxIpCount=").append(degradeMaxIpCount);
        sb.append('}');
        return sb.toString();
    }
}