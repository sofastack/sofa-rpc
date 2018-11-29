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
package com.alipay.sofa.rpc.transport.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.27 4:07 PM
 */
public class GrpcChannelPool {
    private final static Map<String, ManagedChannel> CHANNEL_POOL  = new HashMap<String, ManagedChannel>();

    private final static String                      KEY_SEPARATOR = "#";

    public static ManagedChannel getManagedChannel(String host, int port) {
        String key = buildKey(host, port);
        ManagedChannel channel = CHANNEL_POOL.get(key);

        if (channel == null) {
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            CHANNEL_POOL.put(key, channel);
        }

        return channel;
    }

    private static String buildKey(String host, int port) {
        return host + KEY_SEPARATOR + port;
    }
}