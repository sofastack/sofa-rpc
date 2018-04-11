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

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extensible;

/**
 * Tracer SPI
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see com.alipay.sofa.rpc.tracer.Tracers
 */
@Extensible
public abstract class Tracer {

    /**
     * 0：开始
     *
     * @param request 调用请求
     */
    public abstract void startRpc(SofaRequest request);

    /**
     * 1：客户端发送请求前
     *
     * @param request 调用请求
     */
    public abstract void clientBeforeSend(SofaRequest request);

    /**
     * 2. 服务端收到请求后
     *
     * @param request 调用请求
     */
    public abstract void serverReceived(SofaRequest request);

    /**
     * 3. 服务端返回请求或者异常
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 异常
     */
    public abstract void serverSend(SofaRequest request, SofaResponse response, Throwable throwable);

    /**
     * 4. 客户端收到响应或者异常
     *
     * @param request   调用请求
     * @param response  调用响应
     * @param throwable 异常
     */
    public abstract void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable);

    /**
     * 记录日志
     *
     * @param profileApp    应用
     * @param code    编码
     * @param message 消息
     */
    public abstract void profile(String profileApp, String code, String message);

    /**
     * 1.1. 客户端异步发送后
     *
     * @param request 请求
     */
    public abstract void clientAsyncAfterSend(SofaRequest request);

    /**
     * 4.3 客户端异步收到响应，做准备，例如设置到上下文
     */
    public abstract void clientAsyncReceivedPrepare();

    /**
     * 检查状态，在结束调用的时候进行调用，防止资源泄露 
     */
    public abstract void checkState();
}
