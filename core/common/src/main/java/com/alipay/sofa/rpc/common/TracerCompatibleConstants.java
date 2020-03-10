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
package com.alipay.sofa.rpc.common;

/**
 * TracerCompatibleConstants
 * <p>
 * 注意:这里面的常量万不可更改,涉及到各种兼容问题,如果更改后果很严重
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class TracerCompatibleConstants {

    // ======================== 以下为透传数据的 key ========================
    /**
     * TraceId 放在透传上下文中的 key
     */
    public static final String TRACE_ID_KEY      = "sofaTraceId";

    /**
     * RpcId 放在透传上下文中的 key
     */
    public static final String RPC_ID_KEY        = "sofaRpcId";

    /**
     * penetrateAttributes 放在透传上下文中的 key
     */
    public static final String PEN_ATTRS_KEY     = "sofaPenAttrs";

    /**
     * penetrateSystemAttr 放在透传上下文中的 key
     */
    public static final String PEN_SYS_ATTRS_KEY = "sysPenAttrs";

    /**
     * 调用方应用名放在透传上下文中的 key
     */
    public static final String CALLER_APP_KEY    = "sofaCallerApp";

    /**
     * 调用方逻辑 ZONE 放在透传上下文中的 key
     */
    public static final String CALLER_ZONE_KEY   = "sofaCallerZone";

    /**
     * 调用方 IDC 放在透传上下文中的 key
     */
    public static final String CALLER_IDC_KEY    = "sofaCallerIdc";

    /**
     * 调用方的IP地址
     */
    public static final String CALLER_IP_KEY     = "sofaCallerIp";

    /***
     * 兼容系统采样透传
     */
    public static final String SAMPLING_MARK     = "samp";

}
