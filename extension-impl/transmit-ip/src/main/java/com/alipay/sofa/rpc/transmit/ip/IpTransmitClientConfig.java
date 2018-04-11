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

/**
 * 转发客户端配置
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class IpTransmitClientConfig {

    private Integer port;

    private String  protocol;

    private String  interfaceId;

    private String  uniqueId;

    private String  appName;

    /**
     * Getter method for property <tt>port</tt>.
     *
     * @return property value of port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Setter method for property <tt>port</tt>.
     *
     * @param port  value to be assigned to property port
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Getter method for property <tt>protocol</tt>.
     *
     * @return property value of protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Setter method for property <tt>protocol</tt>.
     *
     * @param protocol  value to be assigned to property protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Getter method for property <tt>interfaceId</tt>.
     *
     * @return property value of interfaceId
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * Setter method for property <tt>interfaceId</tt>.
     *
     * @param interfaceId  value to be assigned to property interfaceId
     */
    public void setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * Getter method for property <tt>appName</tt>.
     *
     * @return property value of appName
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Setter method for property <tt>appName</tt>.
     *
     * @param appName  value to be assigned to property appName
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Getter method for property <tt>uniqueId</tt>.
     *
     * @return property value of uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Setter method for property <tt>uniqueId</tt>.
     *
     * @param uniqueId  value to be assigned to property uniqueId
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Getter method for property <tt>timeOut</tt>.
     *
     * @return property value of timeOut
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IpTransmitClientConfig that = (IpTransmitClientConfig) o;

        if (!port.equals(that.port)) {
            return false;
        }
        if (!protocol.equals(that.protocol)) {
            return false;
        }
        if (!interfaceId.equals(that.interfaceId)) {
            return false;
        }
        if (uniqueId != null ? !uniqueId.equals(that.uniqueId) : that.uniqueId != null) {
            return false;
        }
        return appName.equals(that.appName);
    }

    @Override
    public int hashCode() {
        int result = port.hashCode();
        result = 31 * result + protocol.hashCode();
        result = 31 * result + interfaceId.hashCode();
        result = 31 * result + (uniqueId != null ? uniqueId.hashCode() : 0);
        result = 31 * result + appName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "IpTransmitClientConfig{" +
            "port=" + port +
            ", protocol='" + protocol + '\'' +
            ", interfaceId='" + interfaceId + '\'' +
            ", uniqueId='" + uniqueId + '\'' +
            ", appName='" + appName + '\'' +
            '}';
    }
}