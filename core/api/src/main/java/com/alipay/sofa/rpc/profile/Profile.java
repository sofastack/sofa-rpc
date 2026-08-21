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
package com.alipay.sofa.rpc.profile;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extensible;

/**
 * RPC调用过程性能数据采集SPI。实现必须快速返回，不得影响RPC主链路。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
@Extensible
public abstract class Profile {

    /**
     * 当前是否需要采集数据。
     *
     * @return 是否启用
     */
    public abstract boolean isEnabled();

    /**
     * 客户端开始一次RPC调用。
     *
     * @param request 调用请求
     */
    public abstract void startRpc(SofaRequest request);

    /**
     * 客户端发送请求前。
     *
     * @param request 调用请求
     */
    public abstract void clientBeforeSend(SofaRequest request);

    /**
     * 客户端发送请求后。
     *
     * @param request 调用请求
     */
    public abstract void clientAfterSend(SofaRequest request);

    /**
     * 客户端收到响应或异常。重试场景可能调用多次。
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 调用异常
     */
    public abstract void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable);

    /**
     * 客户端结束一次RPC调用。
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 调用异常
     */
    public abstract void clientEnd(SofaRequest request, SofaResponse response, Throwable throwable);

    /**
     * 服务端收到请求。
     *
     * @param request 调用请求
     */
    public abstract void serverReceived(SofaRequest request);

    /**
     * 服务端发送响应或异常。
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 调用异常
     */
    public abstract void serverSend(SofaRequest request, SofaResponse response, Throwable throwable);

    /**
     * 记录当前RPC调用的一个阶段耗时。阶段数据必须绑定到传入的调用级内部上下文，
     * 不能依赖调用线程的共享上下文。
     *
     * @param context       RPC调用级内部上下文
     * @param phase         阶段标识
     * @param elapsedNanos  阶段耗时，单位纳秒
     */
    public void recordPhase(RpcInternalContext context, String phase, long elapsedNanos) {
        // 默认不记录，具体Profile实现按需处理。
    }

    /**
     * 调用结束时检查并清理未完成的采集状态。
     */
    public abstract void checkState();
}
