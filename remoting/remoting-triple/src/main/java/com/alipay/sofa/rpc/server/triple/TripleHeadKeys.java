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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.common.RemotingConstants;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanggeng on 2017/3/7.
 */
public class TripleHeadKeys {

    protected static ConcurrentHashMap<String, Key<String>> keyMap                    = new ConcurrentHashMap<String, Key<String>>();

    public static final Key<String>                         HEAD_KEY_TARGET_SERVICE   = Key
                                                                                          .of(RemotingConstants.HEAD_TARGET_SERVICE,
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_METHOD_NAME      = Key
                                                                                          .of(RemotingConstants.HEAD_METHOD_NAME,
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_TARGET_APP       = Key
                                                                                          .of(RemotingConstants.HEAD_TARGET_APP,
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_SERVICE_VERSION  = Key
                                                                                          .of("tri-service-version",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_TRACE_ID         = Key
                                                                                          .of("tri-trace-traceid",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_RPC_ID           = Key
                                                                                          .of("tri-trace-rpcid",

                                                                                              Metadata.ASCII_STRING_MARSHALLER);
    //will be lowercase in http2
    public static final Key<String>                         HEAD_KEY_OLD_TRACE_ID     = Key
                                                                                          .of("SOFA-TraceId",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);
    //will be lowercase in http2

    public static final Key<String>                         HEAD_KEY_OLD_RPC_ID       = Key
                                                                                          .of("SOFA-RpcId",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_META_TYPE        = Key
                                                                                          .of("tri-type",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_SAMP_TYPE        = Key
                                                                                          .of("tri-trace-samp",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);
    public static final Key<String>                         HEAD_KEY_CURRENT_APP      = Key
                                                                                          .of("tri-trace-current-app",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_INVOKE_TYPE      = Key
                                                                                          .of("tri-trace-invoke-type",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_PROTOCOL_TYPE    = Key
                                                                                          .of("tri-trace-protocol-type",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);
    public static final Key<String>                         HEAD_KEY_BIZ_BAGGAGE_TYPE = Key
                                                                                          .of("tri-trace-biz-baggage",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);
    public static final Key<String>                         HEAD_KEY_SYS_BAGGAGE_TYPE = Key
                                                                                          .of("tri-trace-sys-baggage",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_UNIT_INFO        = Key
                                                                                          .of("tri-unit-info",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_SOURCE_TENANTID  = Key
                                                                                          .of("tri-tenantid",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    public static final Key<String>                         HEAD_KEY_TARGET_TENANTID  = Key
                                                                                          .of("tri-target-tenantid",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    //for auth
    public static final Key<String>                         HEAD_KEY_CONSUMER_APP     = Key
                                                                                          .of("tri-consumer-appname",
                                                                                              Metadata.ASCII_STRING_MARSHALLER);

    //for auth
    public static final Key<String>                         HEAD_KEY_TRAFFIC_TYPE     = Key
                                                                                          .of("tri-traffic-type",
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
