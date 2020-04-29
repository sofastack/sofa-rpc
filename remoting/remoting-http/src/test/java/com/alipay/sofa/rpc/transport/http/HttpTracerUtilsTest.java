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
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class HttpTracerUtilsTest {

    @Test
    public void isTracerKey() {
        Assert.assertTrue(HttpTracerUtils.isTracerKey(RemotingConstants.RPC_TRACE_NAME + ".xx"));
        Assert.assertFalse(HttpTracerUtils.isTracerKey("rpc_trace_context111.xx"));
    }

    @Test
    public void parseTraceKey() {
        Map<String, String> map = new HashMap<String, String>();
        HttpTracerUtils.parseTraceKey(map, RemotingConstants.RPC_TRACE_NAME + ".xx", "11");
        Assert.assertEquals("11", map.get("xx"));

        HttpTracerUtils.parseTraceKey(map, RemotingConstants.RPC_TRACE_NAME + "." + RemotingConstants.RPC_ID_KEY,
            "11");
        Assert.assertEquals("11", map.get(RemotingConstants.RPC_ID_KEY));
        HttpTracerUtils.parseTraceKey(map, RemotingConstants.RPC_TRACE_NAME + "."
            + RemotingConstants.RPC_ID_KEY.toLowerCase(), "11");
        Assert.assertEquals("11", map.get(RemotingConstants.RPC_ID_KEY));
    }
}