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
 * 资源度量结果描述
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class MeasureResultDetail {
    private final InvocationStatDimension invocationStatDimension;
    private final MeasureState            measureState;

    private long                          timeWindow;
    private long                          windowCount;
    private long                          leastWindowCount;
    private double                        abnormalRate;
    private double                        averageAbnormalRate;
    private double                        leastAbnormalRateMultiple;
    private boolean                       recoveredOriginWeight;
    private transient boolean             logOnly;

    /**
     * Instantiates a new Measure result detail.
     *
     * @param invocation   the invocation
     * @param measureState the measure state
     */
    public MeasureResultDetail(InvocationStatDimension invocation, MeasureState measureState) {
        this.invocationStatDimension = invocation;
        this.measureState = measureState;
    }

    /**
     * Getter method for property <tt>invocationStatDimension</tt>.
     *
     * @return property value of invocationStatDimension
     */
    public InvocationStatDimension getInvocationStatDimension() {
        return invocationStatDimension;
    }

    /**
     * Getter method for property <tt>measureState</tt>.
     *
     * @return property value of measureState
     */
    public MeasureState getMeasureState() {
        return measureState;
    }

    /**
     * Getter method for property <tt>timeWindow</tt>.
     *
     * @return property value of timeWindow
     */
    public long getTimeWindow() {
        return timeWindow;
    }

    /**
     * Setter method for property <tt>timeWindow</tt>.
     *
     * @param timeWindow value to be assigned to property timeWindow
     */
    public void setTimeWindow(long timeWindow) {
        this.timeWindow = timeWindow;
    }

    /**
     * Getter method for property <tt>leastWindowCount</tt>.
     *
     * @return property value of leastWindowCount
     */
    public long getLeastWindowCount() {
        return leastWindowCount;
    }

    /**
     * Setter method for property <tt>leastWindowCount</tt>.
     *
     * @param leastWindowCount value to be assigned to property leastWindowCount
     */
    public void setLeastWindowCount(long leastWindowCount) {
        this.leastWindowCount = leastWindowCount;
    }

    /**
     * Getter method for property <tt>windowCount</tt>.
     *
     * @return property value of windowCount
     */
    public long getWindowCount() {
        return windowCount;
    }

    /**
     * Setter method for property <tt>windowCount</tt>.
     *
     * @param windowCount value to be assigned to property windowCount
     */
    public void setWindowCount(long windowCount) {
        this.windowCount = windowCount;
    }

    /**
     * Getter method for property <tt>abnormalRate</tt>.
     *
     * @return property value of abnormalRate
     */
    public double getAbnormalRate() {
        return abnormalRate;
    }

    /**
     * Setter method for property <tt>abnormalRate</tt>.
     *
     * @param abnormalRate value to be assigned to property abnormalRate
     */
    public void setAbnormalRate(double abnormalRate) {
        this.abnormalRate = abnormalRate;
    }

    /**
     * Getter method for property <tt>averageAbnormalRate</tt>.
     *
     * @return property value of averageAbnormalRate
     */
    public double getAverageAbnormalRate() {
        return averageAbnormalRate;
    }

    /**
     * Setter method for property <tt>averageAbnormalRate</tt>.
     *
     * @param averageAbnormalRate value to be assigned to property averageAbnormalRate
     */
    public void setAverageAbnormalRate(double averageAbnormalRate) {
        this.averageAbnormalRate = averageAbnormalRate;
    }

    /**
     * Getter method for property <tt>leastAbnormalRateMultiple</tt>.
     *
     * @return property value of leastAbnormalRateMultiple
     */
    public double getLeastAbnormalRateMultiple() {
        return leastAbnormalRateMultiple;
    }

    /**
     * Setter method for property <tt>leastAbnormalRateMultiple</tt>.
     *
     * @param leastAbnormalRateMultiple value to be assigned to property leastAbnormalRateMultiple
     */
    public void setLeastAbnormalRateMultiple(double leastAbnormalRateMultiple) {
        this.leastAbnormalRateMultiple = leastAbnormalRateMultiple;
    }

    /**
     * Getter method for property <tt>recoveredOriginWeight</tt>.
     *
     * @return property value of recoveredOriginWeight
     */
    public boolean isRecoveredOriginWeight() {
        return recoveredOriginWeight;
    }

    /**
     * Setter method for property <tt>recoveredOriginWeight</tt>.
     *
     * @param recoveredOriginWeight value to be assigned to property recoveredOriginWeight
     */
    public void setRecoveredOriginWeight(boolean recoveredOriginWeight) {
        this.recoveredOriginWeight = recoveredOriginWeight;
    }

    /**
     * Is log only boolean.
     *
     * @return the boolean
     */
    public boolean isLogOnly() {
        return logOnly;
    }

    /**
     * Sets log only.
     *
     * @param logOnly the log only
     * @return the log only
     */
    public MeasureResultDetail setLogOnly(boolean logOnly) {
        this.logOnly = logOnly;
        return this;
    }
}