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
package com.alipay.sofa.rpc.transport.rest;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.ReflectCache;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.event.ClientBeforeSendEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transport.AbstractProxyClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Rest proxy client transport
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("rest")
public class RestClientTransport extends AbstractProxyClientTransport {
    public RestClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
    }

    @Override
    protected Object buildProxy(ClientTransportConfig transportConfig) throws SofaRpcException {
        SofaResteasyClientBuilder builder = new SofaResteasyClientBuilder();

        ResteasyClient client = builder
            .registerProvider().logProviders()
            .establishConnectionTimeout(transportConfig.getConnectTimeout(), TimeUnit.MILLISECONDS)
            .socketTimeout(transportConfig.getInvokeTimeout(), TimeUnit.MILLISECONDS)
            .connectionPoolSize(transportConfig.getConnectionNum())// 连接池？
            .build();

        ProviderInfo provider = transportConfig.getProviderInfo();
        String url = "http://" + provider.getHost() + ":" + provider.getPort()
            + "/" + StringUtils.trimToEmpty(provider.getPath());
        ResteasyWebTarget target = client.target(url);
        return target.proxy(ClassUtils.forName(transportConfig.getConsumerConfig().getInterfaceId()));
    }

    @Override
    protected Method getMethod(SofaRequest request) throws SofaRpcException {
        return ReflectCache.getOrInitServiceMethod(request.getTargetServiceUniqueName(), request.getMethodName(),
            request.getMethodArgSigs(), true, request.getInterfaceName());
    }

    /**
     * 调用前设置一些属性
     *
     * @param context RPC上下文
     * @param request 请求对象
     */
    @Override
    protected void beforeSend(RpcInternalContext context, SofaRequest request) {
        super.beforeSend(context, request);
        if (EventBus.isEnable(ClientBeforeSendEvent.class)) {
            EventBus.post(new ClientBeforeSendEvent(request));
        }
    }
}