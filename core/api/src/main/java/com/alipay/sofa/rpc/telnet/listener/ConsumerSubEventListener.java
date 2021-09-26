package com.alipay.sofa.rpc.telnet.listener;

import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.Event;
import com.alipay.sofa.rpc.event.Subscriber;
import com.alipay.sofa.rpc.telnet.cache.ConsumerConfigRepository;

public class ConsumerSubEventListener extends Subscriber {
    public ConsumerSubEventListener() {
    }

    @Override
    public void onEvent(Event originEvent) {
        Class eventClass = originEvent.getClass();
        if (eventClass == ConsumerSubEvent.class) {
            ConsumerSubEvent event = (ConsumerSubEvent)originEvent;
            ConsumerConfigRepository consumerConfigRepository = ConsumerConfigRepository.getConsumerConfigRepository();
            consumerConfigRepository.addConsumerConfig(event.getConsumerConfig());
        }
    }
}
