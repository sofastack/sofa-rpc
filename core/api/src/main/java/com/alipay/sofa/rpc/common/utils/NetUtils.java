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

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 网络操作工具类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class NetUtils {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER   = LoggerFactory.getLogger(NetUtils.class);

    /**
     * 最小端口
     */
    private static final int    MIN_PORT = 0;
    /**
     * 最大端口
     */
    private static final int    MAX_PORT = 65535;

    /**
     * 判断端口是否有效 0-65535
     *
     * @param port 端口
     * @return 是否有效
     */
    public static boolean isInvalidPort(int port) {
        return port > MAX_PORT || port < MIN_PORT;
    }

    /**
     * 判断端口是否随机端口 小于0表示随机
     *
     * @param port 端口
     * @return 是否随机端口
     */
    public static boolean isRandomPort(int port) {
        return port == -1;
    }

    /**
     * 检查当前指定端口是否可用，不可用则自动+1再试（随机端口从默认端口开始检查）
     *
     * @param host 当前ip地址
     * @param port 当前指定端口
     * @return 从指定端口开始后第一个可用的端口
     */
    public static int getAvailablePort(String host, int port) {
        return getAvailablePort(host, port, MAX_PORT);
    }

    /**
     * 检查当前指定端口是否可用，不可用则自动+1再试（随机端口从默认端口开始检查）
     *
     * @param host    当前ip地址
     * @param port    当前指定端口
     * @param maxPort 最大端口
     * @return 从指定端口开始后第一个可用的端口
     */
    public static int getAvailablePort(String host, int port, int maxPort) {
        if (isAnyHost(host)
            || isLocalHost(host)
            || isHostInNetworkCard(host)) {
            if (port < MIN_PORT) {
                port = MIN_PORT;
            }
            for (int i = port; i <= maxPort; i++) {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket();
                    ss.bind(new InetSocketAddress(host, i));
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("ip:{} port:{} is available", host, i);
                    }
                    return i;
                } catch (IOException e) {
                    // continue
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Can't bind to address [{}:{}], " +
                            "Maybe 1) The port has been bound. " +
                            "2) The network card of this host is not exists or disable. " +
                            "3) The host is wrong.", host, i);
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Begin try next port(auto +1):{}", i + 1);
                    }
                } finally {
                    IOUtils.closeQuietly(ss);
                }
            }
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_BIND_PORT_ERROR, host));
        } else {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HOST_NOT_FOUND, host));
        }
    }

    /**
     * 任意地址
     */
    public static final String   ANYHOST          = "0.0.0.0";
    /**
     * 本机地址正则
     */
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    /**
     * IPv4地址
     */
    public static final Pattern  IPV4_PATTERN     = Pattern
                                                      .compile(
                                                      "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    /**
     * 是否本地地址 127.x.x.x 或者 localhost
     *
     * @param host 地址
     * @return 是否本地地址
     */
    public static boolean isLocalHost(String host) {
        return StringUtils.isNotBlank(host)
            && (LOCAL_IP_PATTERN.matcher(host).matches() || "localhost".equalsIgnoreCase(host));
    }

    /**
     * 是否默认地址 0.0.0.0
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isAnyHost(String host) {
        return ANYHOST.equals(host);
    }

    /**
     * 是否IPv4地址 0.0.0.0
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isIPv4Host(String host) {
        return StringUtils.isNotBlank(host)
            && IPV4_PATTERN.matcher(host).matches();
    }

    /**
     * 是否非法地址（本地或默认）
     *
     * @param host 地址
     * @return 是否非法地址
     */
    static boolean isInvalidLocalHost(String host) {
        return StringUtils.isBlank(host)
            || isAnyHost(host)
            || isLocalHost(host);
    }

    /**
     * 是否合法地址（非本地，非默认的IPv4地址）
     *
     * @param address InetAddress
     * @return 是否合法
     */
    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null
            && !isAnyHost(name)
            && !isLocalHost(name)
            && isIPv4Host(name));
    }

    /**
     * 是否网卡上的地址
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isHostInNetworkCard(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 得到本机IPv4地址
     *
     * @return ip地址
     */
    public static String getLocalIpv4() {
        InetAddress address = getLocalAddress();
        return address == null ? null : address.getHostAddress();
    }

    /**
     * 遍历本地网卡，返回第一个合理的IP，保存到缓存中
     *
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Error when retrieving ip address: " + e.getMessage(), e);
            }
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    return address;
                                }
                            } catch (Throwable e) {
                                if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn("Error when retrieving ip address: " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Error when retrieving ip address: " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Error when retrieving ip address: " + e.getMessage(), e);
            }
        }
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_GET_HOST_FAIL));
        }
        return localAddress;
    }

    /**
     * InetSocketAddress转 host:port 字符串
     *
     * @param address InetSocketAddress转
     * @return host:port 字符串
     */
    public static String toAddressString(InetSocketAddress address) {
        if (address == null) {
            return StringUtils.EMPTY;
        } else {
            return toIpString(address) + ":" + address.getPort();
        }
    }

    /**
     * 得到ip地址
     *
     * @param address InetSocketAddress
     * @return ip地址
     */
    public static String toIpString(InetSocketAddress address) {
        if (address == null) {
            return null;
        } else {
            InetAddress inetAddress = address.getAddress();
            return inetAddress == null ? address.getHostName() :
                inetAddress.getHostAddress();
        }
    }

    /**
     * 本地多ip情况下、连一下注册中心地址得到本地IP地址
     *
     * @param registryIp 注册中心地址
     * @return 本地多ip情况下得到本地能连上注册中心的IP地址
     */
    public static String getLocalHostByRegistry(String registryIp) {
        String host = null;
        if (registryIp != null && registryIp.length() > 0) {
            List<InetSocketAddress> addrs = getIpListByRegistry(registryIp);
            for (int i = 0; i < addrs.size(); i++) {
                InetAddress address = getLocalHostBySocket(addrs.get(i));
                if (address != null) {
                    host = address.getHostAddress();
                    if (host != null && !NetUtils.isInvalidLocalHost(host)) {
                        return host;
                    }
                }
            }
        }
        if (NetUtils.isInvalidLocalHost(host)) {
            host = NetUtils.getLocalIpv4();
        }
        return host;
    }

    /**
     * 通过连接远程地址得到本机内网地址
     *
     * @param remoteAddress 远程地址
     * @return 本机内网地址
     */
    private static InetAddress getLocalHostBySocket(InetSocketAddress remoteAddress) {
        InetAddress host = null;
        try {
            // 去连一下远程地址
            Socket socket = new Socket();
            try {
                socket.connect(remoteAddress, 1000);
                // 得到本地地址
                host = socket.getLocalAddress();
            } finally {
                IOUtils.closeQuietly(socket);
            }
        } catch (Exception e) {
            LOGGER.warn("Can not connect to host {}, cause by :{}",
                remoteAddress.toString(), e.getMessage());
        }
        return host;
    }

    /**
     * 解析注册中心地址配置为多个连接地址
     *
     * @param registryIp 注册中心地址
     * @return 可以连接注册中心的本机IP
     */
    public static List<InetSocketAddress> getIpListByRegistry(String registryIp) {
        List<String[]> ips = new ArrayList<String[]>();
        String defaultPort = null;

        String[] srcIps = registryIp.split(",");
        for (String add : srcIps) {
            int a = add.indexOf("://");
            if (a > -1) {
                add = add.substring(a + 3); // 去掉协议头
            }
            String[] s1 = add.split(":");
            if (s1.length > 1) {
                if (defaultPort == null && s1[1] != null && s1[1].length() > 0) {
                    defaultPort = s1[1];
                }
                ips.add(new String[] { s1[0], s1[1] }); // 得到ip和端口
            } else {
                ips.add(new String[] { s1[0], defaultPort });
            }
        }

        List<InetSocketAddress> ads = new ArrayList<InetSocketAddress>();
        for (int j = 0; j < ips.size(); j++) {
            String[] ip = ips.get(j);
            try {
                InetSocketAddress address = new InetSocketAddress(ip[0],
                    Integer.parseInt(ip[1] == null ? defaultPort : ip[1]));
                ads.add(address);
            } catch (Exception ignore) { //NOPMD
            }
        }

        return ads;
    }

    /**
     * 判断当前ip是否符合白名单
     *
     * @param whiteList 白名单，可以配置为*
     * @param localIP   当前地址
     * @return 是否在名单里
     */
    public static boolean isMatchIPByPattern(String whiteList, String localIP) {
        if (StringUtils.isNotBlank(whiteList)) {
            if (StringUtils.ALL.equals(whiteList)) {
                return true;
            }
            for (String ips : whiteList.replace(',', ';').split(";", -1)) {
                try {
                    if (ips.contains(StringUtils.ALL)) { // 带通配符
                        String regex = ips.trim().replace(".", "\\.").replace("*", ".*");
                        Pattern pattern = Pattern.compile(regex);
                        if (pattern.matcher(localIP).find()) {
                            return true;
                        }
                    } else if (!isIPv4Host(ips)) { // 不带通配符的正则表达式
                        String regex = ips.trim().replace(".", "\\.");
                        Pattern pattern = Pattern.compile(regex);
                        if (pattern.matcher(localIP).find()) {
                            return true;
                        }
                    } else {
                        if (ips.equals(localIP)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("syntax of pattern {} is invalid", ips);
                }
            }
        }
        return false;
    }

    /**
     * 连接转字符串
     *
     * @param local  本地地址
     * @param remote 远程地址
     * @return 地址信息字符串
     */
    public static String connectToString(InetSocketAddress local, InetSocketAddress remote) {
        return toAddressString(local) + " <-> " + toAddressString(remote);
    }

    /**
     * 连接转字符串
     *
     * @param local1  本地地址
     * @param remote1 远程地址
     * @return 地址信息字符串
     */
    public static String channelToString(SocketAddress local1, SocketAddress remote1) {
        try {
            InetSocketAddress local = (InetSocketAddress) local1;
            InetSocketAddress remote = (InetSocketAddress) remote1;
            return toAddressString(local) + " -> " + toAddressString(remote);
        } catch (Exception e) {
            return local1 + "->" + remote1;
        }
    }

    /**
     * 是否可以telnet
     *
     * @param ip      远程地址
     * @param port    远程端口
     * @param timeout 连接超时
     * @return 是否可连接
     */
    public static boolean canTelnet(String ip, int port, int timeout) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return socket.isConnected() && !socket.isClosed();
        } catch (Exception e) {
            return false;
        } finally {
            IOUtils.closeQuietly(socket);
        }
    }

    /**
     *
     * @param multicastSocket
     * @param multicastAddress
     */
    public static void joinMulticastGroup(MulticastSocket multicastSocket, InetAddress multicastAddress)
        throws IOException {
        setInterface(multicastSocket, multicastAddress instanceof Inet6Address);
        multicastSocket.setLoopbackMode(false);
        multicastSocket.joinGroup(multicastAddress);
    }

    public static void setInterface(MulticastSocket multicastSocket, boolean preferIpv6) throws IOException {
        boolean interfaceSet = false;
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) interfaces.nextElement();
            Enumeration addresses = i.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = (InetAddress) addresses.nextElement();
                if (preferIpv6 && address instanceof Inet6Address) {
                    try {
                        if (address.isReachable(100)) {
                            multicastSocket.setInterface(address);
                            interfaceSet = true;
                            break;
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                } else if (!preferIpv6 && address instanceof Inet4Address) {
                    try {
                        if (address.isReachable(100)) {
                            multicastSocket.setInterface(address);
                            interfaceSet = true;
                            break;
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            if (interfaceSet) {
                break;
            }
        }
    }

}
