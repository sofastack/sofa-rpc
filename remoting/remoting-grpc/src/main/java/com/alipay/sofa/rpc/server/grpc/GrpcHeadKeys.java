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
package com.alipay.sofa.rpc.server.grpc;

import com.alipay.sofa.rpc.common.RemotingConstants;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanggeng on 2017/3/7.
 */
public class GrpcHeadKeys {

    protected static ConcurrentHashMap<String, Key<String>> keyMap                  = new ConcurrentHashMap<String, Key<String>>();

    public static final Key<String>                         HEAD_KEY_TARGET_SERVICE = Key
                                                                                        .of(RemotingConstants.HEAD_TARGET_SERVICE,
                                                                                            Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_METHOD_NAME    = Key
                                                                                        .of(RemotingConstants.HEAD_METHOD_NAME,
                                                                                            Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_TARGET_APP     = Key
                                                                                        .of(RemotingConstants.HEAD_TARGET_APP,
                                                                                            Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_TRACE_ID_APP   = Key
                                                                                        .of("new_rpc_trace_context",
                                                                                            Metadata.ASCII_STRING_MARSHALLER);

    public static Key<String> getKey(String key) {
        Key<String> headKey = keyMap.get(key);
        if (headKey == null) {
            headKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            Key<String> old = keyMap.putIfAbsent(key, headKey);
            if (old != null) {
                headKey = old;
            }
        }
        return headKey;
    }
}
