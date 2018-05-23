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
package com.alipay.sofa.rpc.tracer.sofatracer.code;

/**
 * TracerResultCode
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class TracerResultCode {

    // ==================== 结果码 ====================
    /**
     * 返回成功
     */
    public static final String RPC_RESULT_SUCCESS                 = "00";
    /**
     * 业务失败
     */
    public static final String RPC_RESULT_BIZ_FAILED              = "01";
    /**
     * RPC逻辑失败
     */
    public static final String RPC_RESULT_RPC_FAILED              = "02";
    /**
     * 超时失败
     */
    public static final String RPC_RESULT_TIMEOUT_FAILED          = "03";
    /**
     * 路由失败
     */
    public static final String RPC_RESULT_ROUTE_FAILED            = "04";

    /* ==================== Tracer异常日志里的异常类型 ==================== */
    /**
     * 业务异常
     */
    public static final String RPC_ERROR_TYPE_BIZ_ERROR           = "biz_error";
    /**
     * 地址路由异常
     */
    public static final String RPC_ERROR_TYPE_ADDRESS_ROUTE_ERROR = "address_route_error";
    /**
     * 序列化异常
     */
    public static final String RPC_ERROR_TYPE_SERIALIZE_ERROR     = "serialize_error";
    /**
     * 超时异常
     */
    public static final String RPC_ERROR_TYPE_TIMEOUT_ERROR       = "timeout_error";
    /**
     * 未知异常
     */
    public static final String RPC_ERROR_TYPE_UNKNOWN_ERROR       = "unknown_error";
}
