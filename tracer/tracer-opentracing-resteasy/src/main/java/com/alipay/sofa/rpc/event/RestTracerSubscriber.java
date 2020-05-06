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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.event.rest.RestServerReceiveEvent;
import com.alipay.sofa.rpc.event.rest.RestServerSendEvent;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.RestTracerAdapter;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class RestTracerSubscriber extends Subscriber {

    @Override
    public void onEvent(Event originEvent) {
        if (!Tracers.isEnable()) {
            return;
        }
        Class eventClass = originEvent.getClass();

        if (eventClass == RestServerReceiveEvent.class) {
            RestServerReceiveEvent event = (RestServerReceiveEvent) originEvent;
            RestTracerAdapter.serverReceived(event.getRequest());
        } else if (eventClass == RestServerSendEvent.class) {
            RestServerSendEvent event = (RestServerSendEvent) originEvent;
            RestTracerAdapter.serverSend(event.getResponse(), event.getThrowable());
        }
    }

}
