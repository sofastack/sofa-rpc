package com.alipay.sofa.rpc.protocol.listener;

import com.alipay.sofa.rpc.listener.ConsumerListener;
import com.alipay.sofa.rpc.protocol.ConsumerConfigRepository;

public class ConsumerSubEventListener implements ConsumerListener {
    public ConsumerSubEventListener() {
    }

    @Override
    public void addConsumerConfig(com.alipay.sofa.rpc.event.ConsumerSubEvent consumerSubEvent) {
        ConsumerConfigRepository consumerConfigRepository = ConsumerConfigRepository.getConsumerConfigRepository();
        consumerConfigRepository.addConsumerConfig(consumerSubEvent.getConsumerConfig());
    }
}
