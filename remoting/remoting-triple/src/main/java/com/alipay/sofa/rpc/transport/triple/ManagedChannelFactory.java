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
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Factory for creating gRPC {@link ManagedChannel} instances.
 *
 * <p>Isolates the {@link NettyChannelBuilder} dependency from
 * {@link TripleClientTransport}, allowing the transport class itself to have no direct
 * reference to grpc transport classes. Once this factory is replaced with a pure-Netty
 * implementation in a future Sprint, {@link TripleClientTransport} will require no changes.
 *
 * <p>This class is package-private and intended only for use by {@link TripleClientTransport}.
 */
class ManagedChannelFactory {

    private final int keepAliveInterval;

    ManagedChannelFactory(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * Creates a new {@link ManagedChannel} connected to the given provider.
     *
     * @param url        the provider address
     * @param interceptor the client header interceptor
     * @return a new managed channel
     */
    ManagedChannel create(ProviderInfo url, ClientInterceptor interceptor) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(url.getHost(), url.getPort());
        builder.usePlaintext();
        builder.disableRetry();
        builder.intercept(interceptor);
        builder.maxInboundMetadataSize(
            RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_METADATA_SIZE));
        builder.maxInboundMessageSize(
            RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE));
        if (keepAliveInterval > 0) {
            builder.keepAliveWithoutCalls(true);
            builder.keepAliveTime(keepAliveInterval, TimeUnit.SECONDS);
        }
        return builder.build();
    }
}
