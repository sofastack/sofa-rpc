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

import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.LookoutSubscriber;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.ServerStartedEvent;
import com.alipay.sofa.rpc.event.ServerStoppedEvent;
import com.alipay.sofa.rpc.ext.Extension;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
@Extension("lookout")
public class LookoutModule implements Module {

    private LookoutSubscriber subscriber;

    @Override
    public boolean needLoad() {
        try {
            Class.forName("com.alipay.lookout.spi.MetricsImporterLocator");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void install() {
        subscriber = new LookoutSubscriber();
        EventBus.register(ClientEndInvokeEvent.class, subscriber);
        EventBus.register(ServerSendEvent.class, subscriber);
        EventBus.register(ServerStartedEvent.class, subscriber);
        EventBus.register(ServerStoppedEvent.class, subscriber);
        EventBus.register(ProviderPubEvent.class, subscriber);
        EventBus.register(ConsumerSubEvent.class, subscriber);

    }

    @Override
    public void uninstall() {
        if (subscriber != null) {
            EventBus.unRegister(ClientEndInvokeEvent.class, subscriber);
            EventBus.unRegister(ServerSendEvent.class, subscriber);
            EventBus.unRegister(ServerStartedEvent.class, subscriber);
            EventBus.unRegister(ServerStoppedEvent.class, subscriber);
            EventBus.unRegister(ProviderPubEvent.class, subscriber);
            EventBus.unRegister(ConsumerSubEvent.class, subscriber);
        }
    }
}