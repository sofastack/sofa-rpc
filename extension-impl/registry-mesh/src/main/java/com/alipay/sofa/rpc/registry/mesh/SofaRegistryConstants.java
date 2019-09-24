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
package com.alipay.sofa.rpc.registry.mesh;

/**
 * Created by zhanggeng on 2017/7/5.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class SofaRegistryConstants {

    /**
     *
     */
    public static final String SERIALIZE_TYPE_KEY          = "_SERIALIZETYPE";

    /**
     * 服务端配置的权重
     *
     * @since 4.10.0
     */
    public static final String WEIGHT_KEY                  = "_WEIGHT";
    /**
     * 服务端配置的预热权重
     *
     * @since 4.10.0
     */
    public static final String WARMUP_WEIGHT_KEY           = "_WARMUPWEIGHT";
    /**
     * 服务端配置的预热时间
     *
     * @since 4.10.0
     */
    public static final String WARMUP_TIME_KEY             = "_WARMUPTIME";

    /**
     * 宿主机机器名
     *
     * @since 4.13.0
     */
    public static final String HOST_MACHINE_KEY            = "_HOSTMACHINE";

    /**
     * 客户端配置重试次数
     *
     * @since 4.10.0
     */
    public static final String RETRIES_KEY                 = "_RETRIES";

    public static final String CONNECTI_TIMEOUT            = "_CONNECTTIMEOUT"; // com.taobao.remoting.TRConstants#CONNECT_TIMEOUT_KEY
    public static final String CONNECTI_NUM                = "_CONNECTIONNUM";  // com.taobao.remoting.TRConstants#CONNECTIONNUM_KEY
    public static final String TIMEOUT                     = "_TIMEOUT";        // com.taobao.remoting.TRConstants#TIMEOUT_KEY

    public static final String IDLE_TIMEOUT                = "_IDLETIMEOUT";    //com.taobao.remoting.TRConstants#IDLE_TIMEOUT_KEY
    public static final String MAX_READ_IDLE               = "_MAXREADIDLETIME"; //com.taobao.remoting.TRConstants#MAX_READ_IDLE_KEY

    public static final String APP_NAME                    = "app_name";

    public static final String SELF_APP_NAME               = "self_app_name";

    public static final String RPC_SERVICE_VERSION         = "v";
    public static final String DEFAULT_RPC_SERVICE_VERSION = "";
    public static final String SOFA4_RPC_SERVICE_VERSION   = "4.0";
    public static final String RPC_REMOTING_PROTOCOL       = "p";

    // 方法级
    public static final String KEY_TIMEOUT                 = "clientTimeout";
    public static final String KEY_RETRIES                 = "retries";
    public static final String SOFA_GROUP_KEY              = "sofa.group";
}
