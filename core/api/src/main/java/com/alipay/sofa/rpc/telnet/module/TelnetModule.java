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
package com.alipay.sofa.rpc.telnet.module;

import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.module.Module;
import com.alipay.sofa.rpc.telnet.NettyTelnetServer;
import com.alipay.sofa.rpc.telnet.listener.ConsumerSubEventListener;
import com.alipay.sofa.rpc.telnet.listener.ProviderPubEventListener;

@Extension("telnet")
public class TelnetModule implements Module {
    private ProviderPubEventListener providerPubEventListener;
    private ConsumerSubEventListener consumerSubEventListener;

    @Override
    public boolean needLoad() {
        return true;
    }

    @Override
    public void install() {
        NettyTelnetServer nettyTelnetServer = new NettyTelnetServer(1234);
        try {
            nettyTelnetServer.open();
            providerPubEventListener = new ProviderPubEventListener();
            consumerSubEventListener = new ConsumerSubEventListener();
            EventBus.register(ProviderPubEvent.class, providerPubEventListener);
            EventBus.register(ConsumerSubEvent.class, consumerSubEventListener);
        } catch (InterruptedException interruptedException) {
            nettyTelnetServer.close();
        }
    }

    @Override
    public void uninstall() {
        if (providerPubEventListener != null) {
            EventBus.unRegister(ProviderPubEvent.class, providerPubEventListener);
        }
        if (consumerSubEventListener != null) {
            EventBus.unRegister(ConsumerSubEvent.class, consumerSubEventListener);
        }

    }
}
