package com.alipay.sofa.rpc.telnet.listener;

import com.alipay.sofa.rpc.event.Event;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.event.Subscriber;
import com.alipay.sofa.rpc.telnet.cache.ProviderConfigRepository;

public class ProviderPubEventListener extends Subscriber {
    public ProviderPubEventListener() {
    }

    @Override
    public void onEvent(Event originEvent) {
        Class eventClass = originEvent.getClass();
        if (eventClass == ProviderPubEvent.class) {
            ProviderPubEvent event = (ProviderPubEvent)originEvent;
            ProviderConfigRepository providerConfigRepository = ProviderConfigRepository.getProviderConfigRepository();
            providerConfigRepository.addProviderConfig(event.getProviderConfig());
        }
    }
}