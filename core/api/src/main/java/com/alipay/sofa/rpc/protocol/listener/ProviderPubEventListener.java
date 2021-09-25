package com.alipay.sofa.rpc.protocol.listener;

import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.listener.ProviderListener;
import com.alipay.sofa.rpc.protocol.ProviderConfigRepository;

public class ProviderPubEventListener implements ProviderListener {
    public ProviderPubEventListener() {
    }

    @Override
    public void addProviderConfig(ProviderPubEvent event) {
        ProviderConfigRepository providerConfigRepository = ProviderConfigRepository.getProviderConfigRepository();
        providerConfigRepository.addProviderConfig(event.getProviderConfig());
    }
}
