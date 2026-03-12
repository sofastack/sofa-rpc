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
package com.alipay.sofa.rpc.tracer.sofatracer.log.type;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for RpcTracerLogEnum
 *
 * @author SOFA-RPC Team
 */
public class RpcTracerLogEnumTest {

    @Test
    public void testEnumValues() {
        Assert.assertEquals(8, RpcTracerLogEnum.values().length);
    }

    @Test
    public void testRpcClientDigest() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_CLIENT_DIGEST;
        Assert.assertEquals("rpc_client_digest_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-client-digest.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_client_digest_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpcServerDigest() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_SERVER_DIGEST;
        Assert.assertEquals("rpc_server_digest_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-server-digest.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_server_digest_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpcClientStat() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_CLIENT_STAT;
        Assert.assertEquals("rpc_client_stat_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-client-stat.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_client_stat_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpcServerStat() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_SERVER_STAT;
        Assert.assertEquals("rpc_server_stat_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-server-stat.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_server_stat_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpcClientEvent() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_CLIENT_EVENT;
        Assert.assertEquals("rpc_client_event_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-client-event.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_client_event_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpcServerEvent() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_SERVER_EVENT;
        Assert.assertEquals("rpc_server_event_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-server-event.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_server_event_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpc2JvmDigest() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_2_JVM_DIGEST;
        Assert.assertEquals("rpc_2_jvm_digest_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-2-jvm-digest.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_2_jvm_digest_rolling", logEnum.getRollingKey());
    }

    @Test
    public void testRpc2JvmStat() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.RPC_2_JVM_STAT;
        Assert.assertEquals("rpc_2_jvm_stat_log_name", logEnum.getLogReverseKey());
        Assert.assertEquals("rpc-2-jvm-stat.log", logEnum.getDefaultLogName());
        Assert.assertEquals("rpc_2_jvm_stat_rolling", logEnum.getRollingKey());
    }
}
