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
import com.alipay.sofa.rpc.event.JfrProfileSubscriber;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.ServerReceiveEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.profile.Profile;
import com.alipay.sofa.rpc.profile.ProfileFactory;
import com.alipay.sofa.rpc.profile.Profiles;

/**
 * 加载JFR Profile扩展并订阅RPC调用过程事件。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
@Extension("jfrProfile")
public class JfrProfileModule implements Module {

    private static final String  PROFILE_NAME = "jfr";

    private static final Logger  LOGGER       = LoggerFactory.getLogger(JfrProfileModule.class);

    private JfrProfileSubscriber subscriber;

    @Override
    public boolean needLoad() {
        return PROFILE_NAME.equals(RpcConfigs.getStringValue(RpcOptions.DEFAULT_PROFILE));
    }

    @Override
    public synchronized void install() {
        if (subscriber != null) {
            return;
        }
        JfrProfileSubscriber newSubscriber = new JfrProfileSubscriber();
        try {
            Profile profile = ProfileFactory.getProfile(PROFILE_NAME);
            Profiles.install(profile);
            register(newSubscriber);
            subscriber = newSubscriber;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Load profile impl success: {}, {}", PROFILE_NAME, profile);
            }
        } catch (Throwable e) {
            unregister(newSubscriber);
            Profiles.uninstall();
            LOGGER.warn(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, Profile.class.getName(), PROFILE_NAME), e);
        }
    }

    @Override
    public synchronized void uninstall() {
        if (subscriber != null) {
            unregister(subscriber);
            subscriber = null;
        }
        Profiles.uninstall();
    }

    private static void register(JfrProfileSubscriber subscriber) {
        EventBus.register(ClientStartInvokeEvent.class, subscriber);
        EventBus.register(ClientBeforeSendEvent.class, subscriber);
        EventBus.register(ClientAfterSendEvent.class, subscriber);
        EventBus.register(ClientSyncReceiveEvent.class, subscriber);
        EventBus.register(ClientAsyncReceiveEvent.class, subscriber);
        EventBus.register(ClientEndInvokeEvent.class, subscriber);
        EventBus.register(ServerReceiveEvent.class, subscriber);
        EventBus.register(ServerSendEvent.class, subscriber);
        EventBus.register(ServerEndHandleEvent.class, subscriber);
    }

    private static void unregister(JfrProfileSubscriber subscriber) {
        EventBus.unRegister(ClientStartInvokeEvent.class, subscriber);
        EventBus.unRegister(ClientBeforeSendEvent.class, subscriber);
        EventBus.unRegister(ClientAfterSendEvent.class, subscriber);
        EventBus.unRegister(ClientSyncReceiveEvent.class, subscriber);
        EventBus.unRegister(ClientAsyncReceiveEvent.class, subscriber);
        EventBus.unRegister(ClientEndInvokeEvent.class, subscriber);
        EventBus.unRegister(ServerReceiveEvent.class, subscriber);
        EventBus.unRegister(ServerSendEvent.class, subscriber);
        EventBus.unRegister(ServerEndHandleEvent.class, subscriber);
    }
}
