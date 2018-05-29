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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.common.RemotingConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class HttpTracerUtils {

    private static final String              PREFIX         = RemotingConstants.RPC_TRACE_NAME + ".";

    private static final Map<String, String> TRACER_KEY_MAP = new HashMap<String, String>();

    static {
        TRACER_KEY_MAP.put(RemotingConstants.HTTP_HEADER_TRACE_ID_KEY.toLowerCase(),
            RemotingConstants.HTTP_HEADER_TRACE_ID_KEY);
        TRACER_KEY_MAP.put(RemotingConstants.HTTP_HEADER_RPC_ID_KEY.toLowerCase(),
            RemotingConstants.HTTP_HEADER_RPC_ID_KEY);
        TRACER_KEY_MAP.put(RemotingConstants.TRACE_ID_KEY.toLowerCase(), RemotingConstants.TRACE_ID_KEY);
        TRACER_KEY_MAP.put(RemotingConstants.RPC_ID_KEY.toLowerCase(), RemotingConstants.RPC_ID_KEY);
        TRACER_KEY_MAP.put(RemotingConstants.PEN_ATTRS_KEY.toLowerCase(), RemotingConstants.PEN_ATTRS_KEY);
    }

    /**
     * Is a tracer key
     *
     * @param key tracer key
     * @return Is a tracer key
     */
    public static boolean isTracerKey(String key) {
        return key.startsWith(PREFIX);
    }

    /**
     * Parse tracer key
     *
     * @param tracerMap tracer map
     * @param key       tracer key
     * @param value     tracer value
     */
    public static void parseTraceKey(Map<String, String> tracerMap, String key, String value) {
        String lowKey = key.substring(PREFIX.length());
        String realKey = TRACER_KEY_MAP.get(lowKey);
        tracerMap.put(realKey == null ? lowKey : realKey, value);
    }
}
