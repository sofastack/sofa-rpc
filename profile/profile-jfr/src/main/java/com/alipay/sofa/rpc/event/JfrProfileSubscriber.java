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

import com.alipay.sofa.rpc.profile.Profiles;

/**
 * 将RPC内部事件转换为JFR Profile回调。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
public class JfrProfileSubscriber extends Subscriber {

    @Override
    public void onEvent(Event originEvent) {
        Class eventClass = originEvent.getClass();
        if (eventClass == ClientStartInvokeEvent.class) {
            Profiles.startRpc(((ClientStartInvokeEvent) originEvent).getRequest());
        } else if (eventClass == ClientBeforeSendEvent.class) {
            Profiles.clientBeforeSend(((ClientBeforeSendEvent) originEvent).getRequest());
        } else if (eventClass == ClientAfterSendEvent.class) {
            Profiles.clientAfterSend(((ClientAfterSendEvent) originEvent).getRequest());
        } else if (eventClass == ClientSyncReceiveEvent.class) {
            ClientSyncReceiveEvent event = (ClientSyncReceiveEvent) originEvent;
            Profiles.clientReceived(event.getRequest(), event.getResponse(), event.getThrowable());
        } else if (eventClass == ClientAsyncReceiveEvent.class) {
            ClientAsyncReceiveEvent event = (ClientAsyncReceiveEvent) originEvent;
            Profiles.clientReceived(event.getRequest(), event.getResponse(), event.getThrowable());
        } else if (eventClass == ClientEndInvokeEvent.class) {
            ClientEndInvokeEvent event = (ClientEndInvokeEvent) originEvent;
            Profiles.clientEnd(event.getRequest(), event.getResponse(), event.getThrowable());
            Profiles.checkState();
        } else if (eventClass == ServerReceiveEvent.class) {
            Profiles.serverReceived(((ServerReceiveEvent) originEvent).getRequest());
        } else if (eventClass == ServerSendEvent.class) {
            ServerSendEvent event = (ServerSendEvent) originEvent;
            Profiles.serverSend(event.getRequest(), event.getResponse(), event.getThrowable());
        } else if (eventClass == ServerEndHandleEvent.class) {
            Profiles.checkState();
        }
    }
}
