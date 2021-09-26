package com.alipay.sofa.rpc.telnet.module;

import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.module.Module;
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
        providerPubEventListener = new ProviderPubEventListener();
        consumerSubEventListener = new ConsumerSubEventListener();
        EventBus.register(ProviderPubEvent.class, providerPubEventListener);
        EventBus.register(ConsumerSubEvent.class, consumerSubEventListener);
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
