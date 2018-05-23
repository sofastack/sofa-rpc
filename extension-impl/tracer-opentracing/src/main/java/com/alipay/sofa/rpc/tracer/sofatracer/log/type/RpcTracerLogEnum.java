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
package com.alipay.sofa.rpc.tracer.sofatracer.log.type;

/**
 * RpcTracerLogEnum
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public enum RpcTracerLogEnum {

    // RPC 日志
    RPC_CLIENT_DIGEST("rpc_client_digest_log_name", "rpc-client-digest.log", "rpc_client_digest_rolling"),
    RPC_SERVER_DIGEST("rpc_server_digest_log_name", "rpc-server-digest.log", "rpc_server_digest_rolling"),
    RPC_CLIENT_STAT("rpc_client_stat_log_name", "rpc-client-stat.log", "rpc_client_stat_rolling"),
    RPC_SERVER_STAT("rpc_server_stat_log_name", "rpc-server-stat.log", "rpc_server_stat_rolling"),
    RPC_2_JVM_DIGEST("rpc_2_jvm_digest_log_name", "rpc-2-jvm-digest.log", "rpc_2_jvm_digest_rolling"),
    RPC_2_JVM_STAT("rpc_2_jvm_stat_log_name", "rpc-2-jvm-stat.log", "rpc_2_jvm_stat_rolling");

    /***
     * 获取保留天数 getLogReverseDay 关键字
     */
    private String logReverseKey;

    /***
     * 默认生成的日志名字 .log 结尾同时作为一个类型
     */
    private String defaultLogName;

    /***
     * 日志的滚动策略
     */
    private String rollingKey;

    RpcTracerLogEnum(String logReverseKey, String defaultLogName, String rollingKey) {
        this.logReverseKey = logReverseKey;
        this.defaultLogName = defaultLogName;
        this.rollingKey = rollingKey;
    }

    public String getLogReverseKey() {
        return logReverseKey;
    }

    public String getDefaultLogName() {
        return defaultLogName;
    }

    public String getRollingKey() {
        return rollingKey;
    }
}
