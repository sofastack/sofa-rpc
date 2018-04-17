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

/**
 * 服务提供者信息的的一些属性
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public final class ProviderInfoAttrs {

    /*=====字段配置=====*/
    /**
     * 配置key:rpcVer
     */
    public static final String ATTR_SERIALIZATION         = RpcConstants.CONFIG_KEY_SERIALIZATION;
    /**
     * 配置key:rpcVersion
     */
    public static final String ATTR_RPC_VERSION           = RpcConstants.CONFIG_KEY_RPC_VERSION;

    /*=====静态态配置=====*/
    /**
     * 配置key:权重
     */
    public static final String ATTR_WEIGHT                = RpcConstants.CONFIG_KEY_WEIGHT;
    /**
     * 静态配置key:appName
     */
    public static final String ATTR_APP_NAME              = RpcConstants.CONFIG_KEY_APP_NAME;
    /**
     * 静态配置key:uniqueId
     */
    public static final String ATTR_UNIQUEID              = RpcConstants.CONFIG_KEY_UNIQUEID;
    /**
     * 静态配置key:source 来源
     */
    public static final String ATTR_SOURCE                = "source";
    /**
     * 静态配置key:crosslang 是否支持跨语言
     */
    public static final String ATTR_CROSSLANG             = "crossLang";
    /**
     * 静态配置key:startTime 启动时间
     */
    public static final String ATTR_START_TIME            = "startTime";

    /**
     * 静态配置key:hostMachine 物理机器
     */
    public static final String ATTR_HOST_MACHINE          = "hostMachine";

    /*=====动态配置=====*/
    /**
     * 动态配置key:interface
     */
    public static final String ATTR_INTERFACE             = RpcConstants.CONFIG_KEY_INTERFACE;
    /**
     * 静态配置key:service
     */
    public static final String ATTR_SERVICE               = "service";
    /**
     * 动态配置key:disabled 节点是否启用
     */
    public static final String ATTR_DISABLED              = "disabled";
    /**
     * 动态配置：超时时间
     */
    public static final String ATTR_TIMEOUT               = RpcConstants.CONFIG_KEY_TIMEOUT;
    /**
     * 动态配置：connections
     */
    public static final String ATTR_CONNECTIONS           = "connections";
    /**
     * 动态配置key:warmupWeight
     */
    public static final String ATTR_WARMUP_WEIGHT         = "warmupWeight";

    /**
     * 动态配置key:warmupTime
     */
    public static final String ATTR_WARMUP_TIME           = "warmupTime";

    /**
     * 动态配置key：warmUpEndTime
     */
    public static final String ATTR_WARM_UP_END_TIME      = "warmupEndTime";

    /**
     * 动态配置key:reconnectCoefficient 重试周期系数：1-5（即5次才真正调一次）
     */
    public static final String ATTR_RC_PERIOD_COEFFICIENT = "reconnectCoefficient";

}
