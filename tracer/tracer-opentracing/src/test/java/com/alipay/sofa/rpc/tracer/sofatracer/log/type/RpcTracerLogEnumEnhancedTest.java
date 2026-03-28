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
 * Enhanced unit tests for RpcTracerLogEnum
 *
 * @author SOFA-RPC Team
 */
public class RpcTracerLogEnumEnhancedTest {

    @Test
    public void testAllEnumValues() {
        RpcTracerLogEnum[] values = RpcTracerLogEnum.values();
        Assert.assertEquals(8, values.length);
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

    @Test
    public void testEnumValueOf() {
        RpcTracerLogEnum logEnum = RpcTracerLogEnum.valueOf("RPC_CLIENT_DIGEST");
        Assert.assertNotNull(logEnum);
        Assert.assertEquals(RpcTracerLogEnum.RPC_CLIENT_DIGEST, logEnum);
    }

    @Test
    public void testEnumOrdinal() {
        Assert.assertEquals(0, RpcTracerLogEnum.RPC_CLIENT_DIGEST.ordinal());
        Assert.assertEquals(1, RpcTracerLogEnum.RPC_SERVER_DIGEST.ordinal());
        Assert.assertEquals(2, RpcTracerLogEnum.RPC_CLIENT_STAT.ordinal());
        Assert.assertEquals(3, RpcTracerLogEnum.RPC_SERVER_STAT.ordinal());
    }

    @Test
    public void testClassInitialization() {
        Assert.assertNotNull(RpcTracerLogEnum.class);
    }

    @Test
    public void testAllLogNamesAreUnique() {
        RpcTracerLogEnum[] values = RpcTracerLogEnum.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                Assert.assertNotEquals(values[i].getDefaultLogName(), values[j].getDefaultLogName());
            }
        }
    }

    @Test
    public void testAllLogReverseKeysAreUnique() {
        RpcTracerLogEnum[] values = RpcTracerLogEnum.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                Assert.assertNotEquals(values[i].getLogReverseKey(), values[j].getLogReverseKey());
            }
        }
    }

    @Test
    public void testAllRollingKeysAreUnique() {
        RpcTracerLogEnum[] values = RpcTracerLogEnum.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                Assert.assertNotEquals(values[i].getRollingKey(), values[j].getRollingKey());
            }
        }
    }

    @Test
    public void testDigestLogTypes() {
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName().contains("digest"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_DIGEST.getDefaultLogName().contains("digest"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_2_JVM_DIGEST.getDefaultLogName().contains("digest"));
    }

    @Test
    public void testStatLogTypes() {
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_STAT.getDefaultLogName().contains("stat"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_STAT.getDefaultLogName().contains("stat"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_2_JVM_STAT.getDefaultLogName().contains("stat"));
    }

    @Test
    public void testEventLogTypes() {
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_EVENT.getDefaultLogName().contains("event"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_EVENT.getDefaultLogName().contains("event"));
    }

    @Test
    public void testClientServerPairing() {
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName().contains("client"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_DIGEST.getDefaultLogName().contains("server"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_STAT.getDefaultLogName().contains("client"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_STAT.getDefaultLogName().contains("server"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_CLIENT_EVENT.getDefaultLogName().contains("client"));
        Assert.assertTrue(RpcTracerLogEnum.RPC_SERVER_EVENT.getDefaultLogName().contains("server"));
    }
}
