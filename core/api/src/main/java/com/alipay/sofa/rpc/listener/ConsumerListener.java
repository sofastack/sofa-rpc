package com.alipay.sofa.rpc.listener;

import com.alipay.sofa.rpc.event.ConsumerSubEvent;

import java.util.EventListener;

public interface ConsumerListener extends EventListener {
    /**
     * 添加订阅服务
     *
     * @param consumerSubEvent
     */
    void addConsumerConfig(ConsumerSubEvent consumerSubEvent);

}
