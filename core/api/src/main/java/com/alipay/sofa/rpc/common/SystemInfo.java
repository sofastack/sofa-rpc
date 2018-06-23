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

import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 * 系统相关信息
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class SystemInfo {

    /**
     * 缓存了本机地址
     */
    private static String  LOCALHOST;
    /**
     * 缓存了物理机地址
     */
    private static String  HOSTMACHINE;
    /**
     * 是否Windows系统
     */
    private static boolean IS_WINDOWS;
    /**
     * 是否Linux系统
     */
    private static boolean IS_LINUX;
    /**
     * 是否MAC系统
     */
    private static boolean IS_MAC;

    static {
        boolean[] os = parseOSName();
        IS_WINDOWS = os[0];
        IS_LINUX = os[1];
        IS_MAC = os[2];

        LOCALHOST = NetUtils.getLocalIpv4();
        HOSTMACHINE = parseHostMachine();
    }

    /**
     * 解析物理机地址
     *
     * @return 物理机地址
     */
    @VisibleForTesting
    static boolean[] parseOSName() {
        boolean[] result = new boolean[] { false, false, false };
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            result[0] = true;
        } else if (osName.contains("linux")) {
            result[1] = true;
        } else if (osName.contains("mac")) {
            result[2] = true;
        }
        return result;
    }

    /**
     * 是否Windows系统
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * 是否Linux系统
     */
    public static Boolean isLinux() {
        return IS_LINUX;
    }

    /**
     * 是否Mac系统
     */
    public static boolean isMac() {
        return IS_MAC;
    }

    /**
     * 得到CPU核心数（dock特殊处理）
     *
     * @return 可用的cpu内核数
     */
    public static int getCpuCores() {
        // 找不到文件或者异常，则去物理机的核心数
        int cpu = RpcConfigs.getIntValue(RpcOptions.SYSTEM_CPU_CORES);
        return cpu > 0 ? cpu : Runtime.getRuntime().availableProcessors();
    }

    /**
     * 得到缓存的本机地址
     *
     * @return 本机地址
     */
    public static String getLocalHost() {
        return LOCALHOST;
    }

    /**
     * 设置本机地址到缓存（一般是多网卡由外部选择后设置）
     *
     * @param localhost 本机地址
     */
    public static void setLocalHost(String localhost) {
        LOCALHOST = localhost;
    }

    /**
     * 解析物理机地址
     *
     * @return 物理机地址
     */
    @VisibleForTesting
    static String parseHostMachine() {
        String hostMachine = System.getProperty("host_machine");
        return StringUtils.isNotEmpty(hostMachine) ? hostMachine : null;
    }

    /**
     * 物理机地址
     *
     * @return 物理机地址
     */
    public static String getHostMachine() {
        return HOSTMACHINE;
    }
}
