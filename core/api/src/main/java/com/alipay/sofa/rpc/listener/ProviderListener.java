package com.alipay.sofa.rpc.listener;

import com.alipay.sofa.rpc.event.ProviderPubEvent;

import java.util.EventListener;

public interface ProviderListener extends EventListener {
    /**
     *添加发布服务
     *
     * @param event
     */
    void addProviderConfig(ProviderPubEvent event);
}
