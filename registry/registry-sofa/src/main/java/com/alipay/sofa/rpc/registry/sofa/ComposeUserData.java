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
package com.alipay.sofa.rpc.registry.sofa;

import com.alipay.sofa.rpc.client.ProviderInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组合式的 UserData from UserData and ConfigData
 * @author bystander
 * @version $Id: ComposeUserData.java, v 0.1 2018年06月09日 11:42 AM bystander Exp $
 */
public class ComposeUserData {

    //组合后的数据
    private Map<String, List<ProviderInfo>> zoneData = new ConcurrentHashMap<String, List<ProviderInfo>>(512);

    //当前 zone
    private String                          localZone;

    public Map<String, List<ProviderInfo>> getZoneData() {
        return zoneData;
    }

    public void setZoneData(Map<String, List<ProviderInfo>> zoneData) {
        this.zoneData = zoneData;
    }

    public String getLocalZone() {
        return localZone;
    }

    public void setLocalZone(String localZone) {
        this.localZone = localZone;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ComposeUserData{");
        sb.append("zoneData=").append(zoneData);
        sb.append(", localZone='").append(localZone).append('\'');
        sb.append('}');
        return sb.toString();
    }
}