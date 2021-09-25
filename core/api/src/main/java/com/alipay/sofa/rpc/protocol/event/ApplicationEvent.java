package com.alipay.sofa.rpc.protocol.event;

import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.protocol.listener.ConsumerSubEventListener;
import com.alipay.sofa.rpc.protocol.listener.ProviderPubEventListener;

import java.util.Iterator;
import java.util.Vector;

public class ApplicationEvent {
    private Vector listeners1=new Vector();
    private Vector listeners2=new Vector();
    public ApplicationEvent() {
        super();
    }
    public void addProviderPubEventListener(ProviderPubEventListener providerPubEventListener){
        listeners1.add(providerPubEventListener);
    }
    public void addConsumerSubEventListener(ConsumerSubEventListener consumerSubEventListener){
        listeners2.add(consumerSubEventListener);
    }
    public void notifyProviderPubEvent(ProviderPubEvent providerPubEvent){
        Iterator<ProviderPubEventListener> it = listeners1.iterator();
        while(it.hasNext()){
            it.next().addProviderConfig(providerPubEvent);
        }
    }
    public void notifyConsumerSubEvent(ConsumerSubEvent consumerSubEvent){
        Iterator<ConsumerSubEventListener> it = listeners2.iterator();
        while(it.hasNext()){
            it.next().addConsumerConfig(consumerSubEvent);
        }
    }
}
