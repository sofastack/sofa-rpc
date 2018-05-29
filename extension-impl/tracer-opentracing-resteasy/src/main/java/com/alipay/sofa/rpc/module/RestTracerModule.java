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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.RestTracerSubscriber;
import com.alipay.sofa.rpc.event.rest.RestServerReceiveEvent;
import com.alipay.sofa.rpc.event.rest.RestServerSendEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.server.rest.TraceRequestFilter;
import com.alipay.sofa.rpc.server.rest.TraceResponseFilter;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.TracerFactory;
import com.alipay.sofa.rpc.transport.rest.TraceClientRequestFilter;
import com.alipay.sofa.rpc.transport.rest.TraceClientResponseFilter;

/**
 * 该模块有两个作用：<br>
 *   - 加载sofaTracer <br>
 *   - 订阅事件<br>
 *           
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension("sofaTracer-resteasy")
public class RestTracerModule implements Module {

    /**
     * Tracer事件订阅
     */
    private RestTracerSubscriber subscriber;

    /**
     * 是否启动Tracer
     *
     * @return 是否开启
     */
    public static boolean isEnable() {
        boolean enable = "sofaTracer".equals(RpcConfigs.getStringValue(RpcOptions.DEFAULT_TRACER));
        if (enable) {
            try {
                ClassUtils.forName("javax.ws.rs.container.ContainerRequestFilter");
                ClassUtils.forName("javax.ws.rs.container.ContainerResponseFilter");
                ClassUtils.forName("org.jboss.resteasy.core.interception.PostMatchContainerRequestContext");
                ClassUtils.forName("org.jboss.resteasy.plugins.server.netty.NettyHttpRequest");
                ClassUtils.forName("org.jboss.resteasy.plugins.server.netty.NettyHttpResponse");
            } catch (Exception e) {
                return false;
            }
        }
        return enable;
    }

    @Override
    public boolean needLoad() {
        return isEnable();
    }

    @Override
    public void install() {
        Tracer tracer = TracerFactory.getTracer("sofaTracer");
        if (tracer != null) {
            subscriber = new RestTracerSubscriber();
            EventBus.register(RestServerReceiveEvent.class, subscriber);
            EventBus.register(RestServerSendEvent.class, subscriber);
        }
        // 注册Tracer相关类
        JAXRSProviderManager.registerInternalProviderClass(TraceRequestFilter.class);
        JAXRSProviderManager.registerInternalProviderClass(TraceResponseFilter.class);
        JAXRSProviderManager.registerInternalProviderClass(TraceClientRequestFilter.class);
        JAXRSProviderManager.registerInternalProviderClass(TraceClientResponseFilter.class);
    }

    @Override
    public void uninstall() {
        if (subscriber != null) {
            EventBus.unRegister(RestServerReceiveEvent.class, subscriber);
            EventBus.unRegister(RestServerSendEvent.class, subscriber);
        }
    }
}
