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
package com.alipay.sofa.rpc.common.utils;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

/**
 * 异常工具类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class ExceptionUtils {

    public static SofaRpcRuntimeException buildRuntime(String configKey, String configValue) {
        String msg = "The value of config " + configKey + " [" + configValue + "] is illegal, please check it";
        return new SofaRpcRuntimeException(msg);
    }

    public static SofaRpcRuntimeException buildRuntime(String configKey, String configValue, String message) {
        String msg = "The value of config " + configKey + " [" + configValue + "] is illegal, " + message;
        return new SofaRpcRuntimeException(msg);
    }

    public static boolean isServerException(SofaRpcException exception) {
        int errorType = exception.getErrorType();
        return errorType >= 100 && errorType < 200;
    }

    public static boolean isClientException(SofaRpcException exception) {
        int errorType = exception.getErrorType();
        return errorType >= 200 && errorType < 300;
    }

    /**
     * 返回堆栈信息（e.printStackTrace()的内容）
     *
     * @param e Throwable
     * @return 异常堆栈信息
     */
    public static String toString(Throwable e) {
        StackTraceElement[] traces = e.getStackTrace();
        StringBuilder sb = new StringBuilder(1024);
        sb.append(e.toString()).append("\n");
        if (traces != null) {
            for (StackTraceElement trace : traces) {
                sb.append("\tat ").append(trace).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 返回消息+简短堆栈信息（e.printStackTrace()的内容）
     *
     * @param e          Throwable
     * @param stackLevel 堆栈层级
     * @return 异常堆栈信息
     */
    public static String toShortString(Throwable e, int stackLevel) {
        StackTraceElement[] traces = e.getStackTrace();
        StringBuilder sb = new StringBuilder(1024);
        sb.append(e.toString()).append("\t");
        if (traces != null) {
            for (int i = 0; i < traces.length; i++) {
                if (i < stackLevel) {
                    sb.append("\tat ").append(traces[i]).append("\t");
                } else {
                    break;
                }
            }
        }
        return sb.toString();
    }
}
