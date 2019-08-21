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
package com.alipay.sofa.rpc.registry.mesh.model;

/**
 * @author bystander
 * @version $Id: PublishServiceRequest.java, v 0.1 2018年04月03日 11:27 AM bystander Exp $
 */
public class UnSubscribeServiceRequest {

    private String serviceName;

    //this should be xxx-pool.alipay.com or  xxx.alipay.com can be null
    private String targetAppAddress;

    //这个值是类似DEFAULT/XFIRE这种，也有可能是tr
    private String protocolType;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTargetAppAddress() {
        return targetAppAddress;
    }

    public void setTargetAppAddress(String targetAppAddress) {
        this.targetAppAddress = targetAppAddress;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UnSubscribeServiceRequest{");
        sb.append("serviceName='").append(serviceName).append('\'');
        sb.append(", targetAppAddress='").append(targetAppAddress).append('\'');
        sb.append(", protocolType='").append(protocolType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}