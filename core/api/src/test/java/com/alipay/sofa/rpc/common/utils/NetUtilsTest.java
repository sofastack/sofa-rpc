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
package com.alipay.sofa.rpc.common.utils;

import com.alipay.sofa.rpc.common.SystemInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class NetUtilsTest {
    @Test
    public void isInvalidPort() throws Exception {
    }

    @Test
    public void isRandomPort() throws Exception {
    }

    @Test
    public void getAvailablePort() throws Exception {
        int port = NetUtils.getAvailablePort("127.0.0.1", 33000);
        Assert.assertTrue(port >= 33000 && port < 65535);
        port = NetUtils.getAvailablePort("0.0.0.0", 33000);
        Assert.assertTrue(port >= 33000 && port < 65535);
        port = NetUtils.getAvailablePort(SystemInfo.getLocalHost(), 33000);
        Assert.assertTrue(port >= 33000 && port < 65535);

        port = NetUtils.getAvailablePort("127.0.0.1", -1);
        Assert.assertTrue(port >= 0 && port < 65535);
    }

    @Test
    public void getAvailablePort1() throws Exception {
        int port = NetUtils.getAvailablePort("127.0.0.1", 33000, 33333);
        Assert.assertTrue(port >= 33000 && port < 33333);
        port = NetUtils.getAvailablePort("0.0.0.0", 33000, 33333);
        Assert.assertTrue(port >= 33000 && port < 33333);
        port = NetUtils.getAvailablePort(SystemInfo.getLocalHost(), 33000, 33333);
        Assert.assertTrue(port >= 33000 && port < 33333);
    }

    @Test
    public void isLocalHost() throws Exception {
    }

    @Test
    public void isAnyHost() throws Exception {
    }

    @Test
    public void isIPv4Host() throws Exception {
    }

    @Test
    public void isInvalidLocalHost() throws Exception {
        Assert.assertTrue(NetUtils.isInvalidLocalHost("0.0.0.0"));
        Assert.assertTrue(NetUtils.isInvalidLocalHost("127.0.0.1"));
        Assert.assertTrue(NetUtils.isInvalidLocalHost(""));
        Assert.assertTrue(NetUtils.isInvalidLocalHost(" "));
        Assert.assertTrue(NetUtils.isInvalidLocalHost(null));
    }

    @Test
    public void isHostInNetworkCard() throws Exception {
    }

    @Test
    public void getLocalIpv4() throws Exception {
    }

    @Test
    public void getLocalAddress() throws Exception {
    }

    @Test
    public void toAddressString() throws Exception {
    }

    @Test
    public void toIpString() throws Exception {
    }

    @Test
    public void getLocalHostByRegistry() throws Exception {
    }

    @Test
    public void getIpListByRegistry() throws Exception {
    }

    @Test
    public void isMatchIPByPattern() throws Exception {
        Assert.assertTrue(NetUtils.isMatchIPByPattern("*", "127.0.0.1"));
        Assert.assertTrue(NetUtils.isMatchIPByPattern("10.1.*", "10.1.1.1"));
        Assert.assertFalse(NetUtils.isMatchIPByPattern("10.1.*", "10.2.1.1"));
        Assert.assertFalse(NetUtils.isMatchIPByPattern("10.1.1.1,10.1.1.2", "10.2.1.1"));
        Assert.assertTrue(NetUtils.isMatchIPByPattern("10.1.1.1,10.1.1.2", "10.1.1.1"));
        Assert.assertTrue(NetUtils.isMatchIPByPattern("10.1.1.1", "10.1.1.1"));
        Assert.assertTrue(NetUtils.isMatchIPByPattern("10.1.1.[1-3]", "10.1.1.1"));
        Assert.assertFalse(NetUtils.isMatchIPByPattern("10.1.1.[1-3]", "10.1.1.4"));
    }

    @Test
    public void connectToString() throws Exception {
    }

    @Test
    public void channelToString() throws Exception {
    }

    @Test
    public void canTelnet() throws Exception {
    }

}