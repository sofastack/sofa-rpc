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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SystemInfoTest {

    @BeforeClass
    public static void beforeClass() {
        SystemInfo systemInfo = new SystemInfo();
    }

    @Test
    public void parseOSName() {
        String old = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "windows7");
            Assert.assertTrue(SystemInfo.parseOSName()[0]);
            System.setProperty("os.name", "linux123");
            Assert.assertTrue(SystemInfo.parseOSName()[1]);
            System.setProperty("os.name", "mac osx123");
            Assert.assertTrue(SystemInfo.parseOSName()[2]);
        } finally {
            if (old == null) {
                System.clearProperty("os.name");
            } else {
                System.setProperty("os.name", old);
            }
        }
    }

    @Test
    public void parseHostMachine() {
        String old = System.getProperty("host_machine");
        try {
            System.setProperty("host_machine", "xxx");
            Assert.assertEquals("xxx", SystemInfo.parseHostMachine());
        } finally {
            if (old == null) {
                System.clearProperty("host_machine");
            } else {
                System.setProperty("host_machine", old);
            }
        }
        Assert.assertTrue(StringUtils.isEmpty(SystemInfo.getHostMachine()));
    }

    @Test
    public void isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        Assert.assertEquals(osName.contains("windows"), SystemInfo.isWindows());
        Assert.assertEquals(osName.contains("linux"), SystemInfo.isLinux());
        Assert.assertEquals(osName.contains("mac"), SystemInfo.isMac());
    }

    @Test
    public void getCpuCores() {
        Assert.assertTrue(SystemInfo.getCpuCores() > 0);
    }

    @Test
    public void getLocalHost() {
        Assert.assertNotNull(SystemInfo.getLocalHost());
    }

    @Test
    public void setLocalHost() {
        String old = SystemInfo.getLocalHost();
        try {
            SystemInfo.setLocalHost("xxx");
            Assert.assertEquals("xxx", SystemInfo.getLocalHost());
        } finally {
            SystemInfo.setLocalHost(old);
        }
    }
}