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

import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 * 转发计算结果
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class IpTransmitResult {
    private boolean transmit        = false;
    private String  transmitAddress = StringUtils.EMPTY;
    private Integer transmitTimeout;

    public void setTransmitAddress(String transmitAddress) {
        this.transmitAddress = transmitAddress;
    }

    public String getTransmitAddress() {
        return transmitAddress;
    }

    public void setTransmit(boolean transmit) {
        this.transmit = transmit;
    }

    public boolean isTransmit() {
        return transmit;
    }

    /**
     * Getter method for property <tt>transmitTimeout</tt>.
     *
     * @return property value of transmitTimeout
     */
    public Integer getTransmitTimeout() {
        return transmitTimeout;
    }

    /**
     * Setter method for property <tt>transmitTimeout</tt>.
     *
     * @param transmitTimeout  value to be assigned to property transmitTimeout
     */
    public void setTransmitTimeout(Integer transmitTimeout) {
        this.transmitTimeout = transmitTimeout;
    }

}