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

import com.alipay.sofa.rpc.dynamic.ConfigChangedEvent;

import java.util.Map;

/**
 * Listener of config for registry.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface ConfigListener {

    /**
     * 处理配置变更事件
     *
     * @param event 配置变更事件
     */
    default void process(ConfigChangedEvent event){
        // do nothing
    }

    /**
     * 配置发生变化，例如
     *
     * @param newValue 新配置
     */
    void configChanged(Map newValue);

    /**
     * 属性发生变化
     *
     * @param newValue 新配置
     */
    void attrUpdated(Map newValue);
}
