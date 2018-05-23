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
import com.alipay.sofa.rpc.event.ClientAfterSendEvent;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientBeforeSendEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ClientStartInvokeEvent;
import com.alipay.sofa.rpc.event.ClientSyncReceiveEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.ServerReceiveEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.SofaTracerSubscriber;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.tracer.Tracer;
import com.alipay.sofa.rpc.tracer.TracerFactory;

/**
 * 该模块有两个作用：<br>
 *   - 加载sofaTracer <br>
 *   - 订阅事件<br>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension("sofaTracer")
public class SofaTracerModule implements Module {

    /**
     * Tracer事件订阅
     */
    private SofaTracerSubscriber subscriber;

    /**
     * 是否启动Tracer
     *
     * @return 是否开启
     */
    public static boolean isEnable() {
        String traceName = RpcConfigs.getStringValue(RpcOptions.DEFAULT_TRACER);
        return "sofaTracer".equals(traceName);
    }

    @Override
    public boolean needLoad() {
        return isEnable();
    }

    @Override
    public void install() {
        Tracer tracer = TracerFactory.getTracer("sofaTracer");
        if (tracer != null) {
            subscriber = new SofaTracerSubscriber();
            EventBus.register(ClientStartInvokeEvent.class, subscriber);
            EventBus.register(ClientBeforeSendEvent.class, subscriber);
            EventBus.register(ClientAfterSendEvent.class, subscriber);
            EventBus.register(ServerReceiveEvent.class, subscriber);
            EventBus.register(ServerSendEvent.class, subscriber);
            EventBus.register(ServerEndHandleEvent.class, subscriber);
            EventBus.register(ClientSyncReceiveEvent.class, subscriber);
            EventBus.register(ClientAsyncReceiveEvent.class, subscriber);
            EventBus.register(ClientEndInvokeEvent.class, subscriber);
        }
    }

    @Override
    public void uninstall() {
        if (subscriber != null) {
            EventBus.unRegister(ClientStartInvokeEvent.class, subscriber);
            EventBus.unRegister(ClientBeforeSendEvent.class, subscriber);
            EventBus.unRegister(ClientAfterSendEvent.class, subscriber);
            EventBus.unRegister(ServerReceiveEvent.class, subscriber);
            EventBus.unRegister(ServerSendEvent.class, subscriber);
            EventBus.unRegister(ServerEndHandleEvent.class, subscriber);
            EventBus.unRegister(ClientSyncReceiveEvent.class, subscriber);
            EventBus.unRegister(ClientAsyncReceiveEvent.class, subscriber);
            EventBus.unRegister(ClientEndInvokeEvent.class, subscriber);
        }
    }
}
