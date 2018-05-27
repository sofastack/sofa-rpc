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

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ClientStartInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.invoke.Invoker;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 客户端引用代理Invoker，一个引用一个。线程安全
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@ThreadSafe
public class ClientProxyInvoker implements Invoker {

    /**
     * 对应的客户端信息
     */
    protected final ConsumerConfig consumerConfig;

    /**
     *
     */
    protected Cluster              cluster;

    /**
     * 构造执行链
     *
     * @param bootstrap 调用端配置
     */
    public ClientProxyInvoker(ConsumerBootstrap bootstrap) {
        this.consumerConfig = bootstrap.getConsumerConfig();
        // 构建客户端
        this.cluster = bootstrap.getCluster();
    }

    /**
     * proxy拦截的调用
     *
     * @param request 请求消息
     * @return 调用结果
     */
    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        SofaResponse response = null;
        Throwable throwable = null;
        try {
            RpcInternalContext.pushContext();
            RpcInternalContext context = RpcInternalContext.getContext();
            context.setProviderSide(false);
            // 包装请求
            decorateRequest(request);
            try {
                // 产生开始调用事件
                if (EventBus.isEnable(ClientStartInvokeEvent.class)) {
                    EventBus.post(new ClientStartInvokeEvent(request));
                }
                // 得到结果
                response = cluster.invoke(request);
            } catch (SofaRpcException e) {
                throwable = e;
                throw e;
            } finally {
                // 产生调用结束事件
                if (!request.isAsync()) {
                    if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                        EventBus.post(new ClientEndInvokeEvent(request, response, throwable));
                    }
                }
            }
            // 包装响应
            decorateResponse(response);
            return response;
        } finally {
            RpcInternalContext.removeContext();
            RpcInternalContext.popContext();
        }
    }

    /**
     * 包装请求
     *
     * @param request 请求
     */
    protected void decorateRequest(SofaRequest request) {
        /* 暂时不支持隐式传参 
        String methodName = request.getMethodName();
         // 将接口的<sofa:param />的配置复制到invocation
         Map params = consumerConfig.getParameters();
         if (params != null) {
             request.addRequestProps(params);
         }
         // 将方法的<sofa:param />的配置复制到invocation
         params = (Map) consumerConfig.getMethodConfigValue(methodName, RpcConstants.CONFIG_KEY_PARAMS);
         if (params != null) {
             request.addRequestProps(params);
         }*/
    }

    /**
     * 包装响应
     *
     * @param response 响应
     */
    protected void decorateResponse(SofaResponse response) { //NOPMD

    }

    /**
     * @return the consumerConfig
     */
    public ConsumerConfig<?> getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * 切换客户端
     *
     * @param newCluster 新客户端
     * @return 旧客户端
     */
    public Cluster setCluster(Cluster newCluster) {
        // 开始切换
        Cluster old = this.cluster;
        this.cluster = newCluster;
        return old;
    }

    /**
     * 获取客户端
     *
     * @return 客户端
     */
    public Cluster getCluster() {
        return this.cluster;
    }
}