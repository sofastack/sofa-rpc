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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.tracer.Tracers;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class SofaTracerSubscriber extends Subscriber {

    @Override
    public void onEvent(Event originEvent) {
        if (!Tracers.isEnable()) {
            return;
        }
        Class eventClass = originEvent.getClass();

        if (eventClass == ClientStartInvokeEvent.class) {
            ClientStartInvokeEvent event = (ClientStartInvokeEvent) originEvent;
            Tracers.startRpc(event.getRequest());
        } else if (eventClass == ClientBeforeSendEvent.class) {
            ClientBeforeSendEvent event = (ClientBeforeSendEvent) originEvent;
            Tracers.clientBeforeSend(event.getRequest());
        } else if (eventClass == ClientAfterSendEvent.class) {
            // 异步发送完毕
            ClientAfterSendEvent event = (ClientAfterSendEvent) originEvent;
            Tracers.clientAsyncAfterSend(event.getRequest());
        }

        // else if (eventClass == ClientSyncReceiveEvent.class) {
        //     // 同步返回结果
        // }

        else if (eventClass == ClientAsyncReceiveEvent.class) {
            ClientAsyncReceiveEvent event = (ClientAsyncReceiveEvent) originEvent;
            // 拿出tracer信息 让入Tracer自己的上下文
            Tracers.clientAsyncReceivedPrepare();
            // 记录收到返回
            Tracers.clientReceived(event.getRequest(), event.getResponse(), event.getThrowable());
        } else if (eventClass == ClientEndInvokeEvent.class) {
            ClientEndInvokeEvent event = (ClientEndInvokeEvent) originEvent;
            if (!event.getRequest().isAsync()) {
                // 因为同步调用重试行为，需要放到最后才能算 received
                Tracers.clientReceived(event.getRequest(), event.getResponse(), event.getThrowable());
            }
            // 检查下状态
            Tracers.checkState();
        } else if (eventClass == ServerReceiveEvent.class) {
            ServerReceiveEvent event = (ServerReceiveEvent) originEvent;
            // 接到请求
            Tracers.serverReceived(event.getRequest());
        } else if (eventClass == ServerSendEvent.class) {
            // 发送响应
            ServerSendEvent event = (ServerSendEvent) originEvent;
            Tracers.serverSend(event.getRequest(), event.getResponse(), event.getThrowable());
        } else if (eventClass == ServerEndHandleEvent.class) {
            // 检查下状态
            Tracers.checkState();
        }
    }
}
