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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;

import java.lang.reflect.Method;

/**
 * Invoker for Grpc
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.12.15 7:06 PM
 */
public class TripleClientInvoker implements TripleInvoker {
    private final static Logger LOGGER = LoggerFactory.getLogger(TripleClientInvoker.class);

    private Channel             channel;

    private ConsumerConfig      consumerConfig;

    private Method              sofaStub;

    public TripleClientInvoker(ConsumerConfig consumerConfig, Channel channel) {
        this.channel = channel;
        this.consumerConfig = consumerConfig;
        Class enclosingClass = consumerConfig.getProxyClass().getEnclosingClass();
        try {
            sofaStub = enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class,
                ProviderInfo.class, ConsumerConfig.class, int.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("getSofaStub not found in enclosingClass" + enclosingClass.getName());
        }
    }

    @Override
    public SofaResponse invoke(SofaRequest sofaRequest, int timeout)
        throws Exception {
        SofaResponse sofaResponse = new SofaResponse();
        ProviderInfo providerInfo = null;
        Object stub = sofaStub.invoke(null, channel, CallOptions.DEFAULT, providerInfo, consumerConfig, timeout);
        final Method method = sofaRequest.getMethod();
        Object appResponse = method.invoke(stub, sofaRequest.getMethodArgs()[0]);
        sofaResponse.setAppResponse(appResponse);
        return sofaResponse;
    }
}