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
package com.alipay.sofa.rpc.server.http;

import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.server.ProviderProxyInvoker;
import com.alipay.sofa.rpc.server.ServerHandler;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class HttpServerHandler implements ServerHandler {

    /**
     * Invoker列表，接口--> Invoker
     */
    protected Map<String, Invoker> invokerMap      = new ConcurrentHashMap<String, Invoker>();

    /**
     * 当前Client正在发送的调用数量
     */
    protected AtomicInteger        processingCount = new AtomicInteger(0);

    /**
     * 业务线程池
     */
    protected ThreadPoolExecutor   bizThreadPool;

    public Map<String, Invoker> getInvokerMap() {
        return invokerMap;
    }

    public AtomicInteger getProcessingCount() {
        return processingCount;
    }

    @Override
    public void registerChannel(AbstractChannel nettyChannel) {

    }

    @Override
    public void unRegisterChannel(AbstractChannel nettyChannel) {

    }

    /**
     * Handle request from HTTP/1.1
     * 
     * @param request SofaRequest
     * @param ctx ChannelHandlerContext
     * @param keepAlive keepAlive
     */
    public void handleHttp1Request(SofaRequest request, ChannelHandlerContext ctx, boolean keepAlive) {
        Http1ServerTask task = new Http1ServerTask(this, request, ctx, keepAlive);

        processingCount.incrementAndGet();
        try {
            task.run();
        } catch (RejectedExecutionException e) {
            processingCount.decrementAndGet();
            throw e;
        }

    }

    /**
     * Handle request from HTTP/2
     * 
     * @param streamId stream Id
     * @param request SofaRequest
     * @param ctx ChannelHandlerContext
     * @param encoder Http2ConnectionEncoder
     */
    public void handleHttp2Request(int streamId, SofaRequest request, ChannelHandlerContext ctx,
                                   Http2ConnectionEncoder encoder) {
        Http2ServerTask task = new Http2ServerTask(this, request, ctx, streamId, encoder);

        processingCount.incrementAndGet();
        try {
            task.run();
        } catch (RejectedExecutionException e) {
            processingCount.decrementAndGet();
            throw e;
        }
    }

    protected boolean isEmpty() {
        return invokerMap == null || invokerMap.isEmpty();
    }

    protected Method getMethod(String serviceName, String methodName) {
        return ReflectCache.getMethodCache(serviceName, methodName);
    }

    /**
     * Check service exists
     *
     * @param serviceName Service Name
     * @param methodName  Method name
     * @return if service and method exists, return true.
     */
    public boolean checkService(String serviceName, String methodName) {
        Invoker invoker = invokerMap.get(serviceName);
        return invoker instanceof ProviderProxyInvoker && getMethod(serviceName, methodName) != null;
    }

    public ThreadPoolExecutor getBizThreadPool() {
        return bizThreadPool;
    }

    public HttpServerHandler setBizThreadPool(ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = bizThreadPool;
        return this;
    }
}