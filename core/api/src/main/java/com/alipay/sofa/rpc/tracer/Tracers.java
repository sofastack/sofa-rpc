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
package com.alipay.sofa.rpc.tracer;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * 包装工具类，自动判断是否启动Tracer。<br>
 * 
 * <pre>
 * Trace主要分为几个阶段：
 * 0：开始
 * 1：客户端发送请求前 （异步要特殊处理）
 *      1.1 异步结束
 * 2. 服务端收到请求后
 * 3. 服务端返回响应后
 *      3.1 返回正常响应
 *      3.2 返回异常响应
 * 4. 客户端收到响应后 （异步要特殊处理）
 *      4.1 收到正常响应
 *      4.2 收到异常响应
 *
 * 其它：
 *  记录profiler日志
 *
 * 同步情况下，1和4在同一个线程，2和3在同一个线程。
 * 异步情况下，1和1.1在同一个线程，2和3在同一个线程，4在一个线程
 * </pre>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public final class Tracers {

    private static final Logger LOGGER    = LoggerFactory.getLogger(Tracers.class);

    /**
     * 是否开启Tracer
     */
    private static boolean      openTrace = false;

    /**
     * Tracer实例，启动时初始化
     */
    private static Tracer       tracer;

    static {
        try {
            String traceName = RpcConfigs.getStringValue(RpcOptions.DEFAULT_TRACER);
            if (StringUtils.isNotBlank(traceName)) {
                tracer = TracerFactory.getTracer(traceName);
                openTrace = true;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Load tracer impl success: {}, {}", traceName, tracer);
                }
            }
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_TRACER_INIT), e);
        }
    }

    public static Tracer getTracer() {
        return tracer;
    }

    /**
     * 是否启动Tracer
     *
     * @return 是否开启
     */
    public static boolean isEnable() {
        return openTrace;
    }

    /**
     * 0：开始
     *
     * @param request 调用请求
     */
    public static void startRpc(SofaRequest request) {
        if (openTrace) {
            try {
                tracer.startRpc(request);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("", e);
                }
            }
        }
    }

    /**
     * 1：客户端发送请求前
     *
     * @param request 调用请求
     */
    public static void clientBeforeSend(SofaRequest request) {
        if (openTrace) {
            try {
                tracer.clientBeforeSend(request);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 2. 服务端收到请求后
     *
     * @param request 调用请求
     */
    public static void serverReceived(SofaRequest request) {
        if (openTrace) {
            try {
                tracer.serverReceived(request);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 3. 服务端返回请求或者异常
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 异常
     */
    public static void serverSend(SofaRequest request, SofaResponse response, Throwable throwable) {
        if (openTrace) {
            try {
                tracer.serverSend(request, response, throwable);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 4. 客户端收到响应或者异常
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 异常
     */
    public static void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable) {
        if (openTrace) {
            try {
                tracer.clientReceived(request, response, throwable);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 1.1. 客户端异步发送后
     *
     * @param request 请求
     */
    public static void clientAsyncAfterSend(SofaRequest request) {
        if (openTrace) {
            try {
                tracer.clientAsyncAfterSend(request);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 4.3 客户端异步收到响应，做准备，例如设置到上下文
     */
    public static void clientAsyncReceivedPrepare() {
        if (openTrace) {
            try {
                tracer.clientAsyncReceivedPrepare();
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 记录日志
     *
     * @param profileApp 应用
     * @param code       编码
     * @param message    消息
     */
    public static void profile(String profileApp, String code, String message) {
        if (openTrace) {
            try {
                tracer.profile(profileApp, code, message);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 检查状态，在结束调用的时候进行调用，防止资源泄露
     */
    public static void checkState() {
        if (openTrace) {
            try {
                tracer.checkState();
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

}
