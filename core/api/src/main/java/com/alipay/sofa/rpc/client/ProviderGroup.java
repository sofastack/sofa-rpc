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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.common.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One provider group contains one list of some providers.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderGroup {

    /**
     * 服务分组名称
     */
    protected final String       name;

    /**
     * 服务分组下服务端列表（缓存的是List，方便快速读取）
     */
    protected List<ProviderInfo> providerInfos;

    /**
     * Instantiates a new Provider group.
     */
    public ProviderGroup() {
        this(RpcConstants.ADDRESS_DEFAULT_GROUP, new ArrayList<ProviderInfo>());
    }

    /**
     * Instantiates a new Provider group.
     *
     * @param name          the name
     */
    public ProviderGroup(String name) {
        this(name, null);
    }

    /**
     * Instantiates a new Provider group.
     *
     * @param name          the name
     * @param providerInfos the provider infos
     */
    public ProviderGroup(String name, List<ProviderInfo> providerInfos) {
        this.name = name;
        this.providerInfos = providerInfos == null ? new ArrayList<ProviderInfo>() : providerInfos;
    }

    /**
     * Instantiates a new Provider group.
     *
     * @param providerInfos the provider infos
     */
    public ProviderGroup(List<ProviderInfo> providerInfos) {
        this(RpcConstants.ADDRESS_DEFAULT_GROUP, providerInfos);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets provider infos.
     *
     * @return the provider infos
     */
    public List<ProviderInfo> getProviderInfos() {
        return providerInfos;
    }

    /**
     * Sets provider infos.
     *
     * @param providerInfos the provider infos
     */
    public void setProviderInfos(List<ProviderInfo> providerInfos) {
        this.providerInfos = providerInfos;
    }

    /**
     * Is empty boolean.
     *
     * @return the boolean
     */
    public boolean isEmpty() {
        return CommonUtils.isEmpty(providerInfos);
    }

    /**
     * Size int.
     *
     * @return the int
     */
    public int size() {
        return providerInfos == null ? 0 : providerInfos.size();
    }

    /**
     * 增加服务列表
     *
     * @param providerInfo 要增加的服务分组列表
     * @return 当前服务分组 provider group
     */
    public ProviderGroup add(ProviderInfo providerInfo) {
        if (providerInfo == null) {
            return this;
        }
        ConcurrentHashSet<ProviderInfo> tmp = new ConcurrentHashSet<ProviderInfo>(providerInfos);
        tmp.add(providerInfo); // 排重
        this.providerInfos = new ArrayList<ProviderInfo>(tmp);
        return this;
    }

    /**
     * 增加多个服务列表
     *
     * @param providerInfos 要增加的服务分组列表
     * @return 当前服务分组 provider group
     */
    public ProviderGroup addAll(Collection<ProviderInfo> providerInfos) {
        if (CommonUtils.isEmpty(providerInfos)) {
            return this;
        }
        ConcurrentHashSet<ProviderInfo> tmp = new ConcurrentHashSet<ProviderInfo>(this.providerInfos);
        tmp.addAll(providerInfos); // 排重
        this.providerInfos = new ArrayList<ProviderInfo>(tmp);
        return this;
    }

    /**
     * 删除服务列表
     *
     * @param providerInfo 要删除的服务分组列表
     * @return 当前服务分组 provider group
     */
    public ProviderGroup remove(ProviderInfo providerInfo) {
        if (providerInfo == null) {
            return this;
        }
        ConcurrentHashSet<ProviderInfo> tmp = new ConcurrentHashSet<ProviderInfo>(providerInfos);
        tmp.remove(providerInfo); // 排重
        this.providerInfos = new ArrayList<ProviderInfo>(tmp);
        return this;
    }

    /**
     * 删除多个服务列表
     *
     * @param providerInfos 要删除的服务分组列表
     * @return 当前服务分组 provider group
     */
    public ProviderGroup removeAll(List<ProviderInfo> providerInfos) {
        if (CommonUtils.isEmpty(providerInfos)) {
            return this;
        }
        ConcurrentHashSet<ProviderInfo> tmp = new ConcurrentHashSet<ProviderInfo>(this.providerInfos);
        tmp.removeAll(providerInfos); // 排重
        this.providerInfos = new ArrayList<ProviderInfo>(tmp);
        return this;
    }

    @Override
    public String toString() {
        return "ProviderGroup{" +
            "name='" + name + '\'' +
            ", providerInfos=" + providerInfos +
            '}';
    }

}
