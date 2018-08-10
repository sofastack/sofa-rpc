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
package com.alipay.sofa.rpc.listener;

import com.alipay.sofa.rpc.client.ProviderGroup;

import java.util.List;

/**
 * Listener of provider info
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface ProviderInfoListener {

    /**
     * 增加某标签的服务端列表 （增量）
     *
     * @param providerGroup 服务端列表组
     */
    void addProvider(ProviderGroup providerGroup);

    /**
     * 删除某标签的服务端列表（增量）
     *
     * @param providerGroup 服务端列表组
     */
    void removeProvider(ProviderGroup providerGroup);

    /**
     * 更新某标签的服务端列表（全量）
     *
     * @param providerGroup 服务端列表组
     */
    void updateProviders(ProviderGroup providerGroup);

    /**
     * 更新全部服务端列表（全量）
     *
     * @param providerGroups 全部服务端列表，为空代表清空已有列表
     */
    void updateAllProviders(List<ProviderGroup> providerGroups);
}
