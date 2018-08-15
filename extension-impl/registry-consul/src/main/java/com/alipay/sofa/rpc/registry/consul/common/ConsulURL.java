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
package com.alipay.sofa.rpc.registry.consul.common;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConsulURL
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class ConsulURL implements Serializable {

    private static final long                         serialVersionUID = -1985165475234910535L;

    private final String                              protocol;

    private final String                              host;

    private final int                                 port;

    private final String                              group;

    private final String                              interfaceId;

    private final String                              path;

    private final Map<String, String>                 parameters;

    // ==== cache ====

    private volatile transient Map<String, Number>    numbers;

    private volatile transient Map<String, ConsulURL> urls;

    private volatile transient String                 ip;

    private volatile transient String                 full;

    private volatile transient String                 identity;

    private volatile transient String                 parameter;

    private volatile transient String                 string;

    protected ConsulURL() {
        this.protocol = null;
        this.host = null;
        this.port = 0;
        this.path = null;
        this.group = null;
        this.interfaceId = null;
        this.parameters = null;
    }

    public ConsulURL(String protocol, String host, int port) {
        this(protocol, host, port, null, "", "", (Map<String, String>) null);
    }

    public ConsulURL(String protocol, String host, int port, String[] pairs) {
        this(protocol, host, port, null, "", "", toStringMap(pairs));
    }

    public ConsulURL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, host, port, "", "", null, parameters);
    }

    public ConsulURL(String protocol, String host, int port, String path) {
        this(protocol, host, port, path, "", "", (Map<String, String>) null);
    }

    public ConsulURL(String protocol, String host, int port, String path, String... pairs) {
        this(protocol, host, port, path, "", "", toStringMap(pairs));
    }

    public ConsulURL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, host, port, path, "", "", parameters);
    }

    public ConsulURL(String protocol, String username, String host, int port, String path) {
        this(protocol, host, port, path, "", "", (Map<String, String>) null);
    }

    public ConsulURL(String protocol, String username, String host, int port, String path,
                     String... pairs) {
        this(protocol, host, port, path, "", "", toStringMap(pairs));
    }

    public ConsulURL(String protocol, String host, int port, String path, String group,
                     String interfaceId,
                     Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = (port < 0 ? 0 : port);
        this.path = path;
        this.interfaceId = interfaceId;
        this.group = group;
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        } else {
            parameters = new HashMap<String, String>(parameters);
        }
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public static ConsulURL valueOf(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String host = null;
        String interfaceId = null;
        String group = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;
        int i = url.indexOf("?"); // seperator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");
            parameters = new HashMap<String, String>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            interfaceId = parameters.get("uniqueId");
            group = parameters.get("group");
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0)
                throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0)
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }
        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0)
            host = url;
        return new ConsulURL(protocol, host, port, path, interfaceId, group, parameters);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getIp() {

        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (UnknownHostException e) {
            return ip;
        }
    }

    public int getPort() {
        return port;
    }

    public int getPort(int defaultPort) {
        return port <= 0 ? defaultPort : port;
    }

    public String getAddress() {
        return port <= 0 ? host : host + ":" + port;
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath() {
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public ConsulURL setProtocol(String protocol) {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public ConsulURL setUsername(String username) {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public ConsulURL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public ConsulURL setHost(String host) {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public ConsulURL setPort(int port) {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public ConsulURL setPath(String path) {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, getParameters());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return decode(getParameter(key, defaultValue));
    }

    public String getParameter(String key) {
        String value = parameters.get(key);
        return value;
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public String[] getParameter(String key, String[] defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return ConsulConstants.COMMA_SPLIT_PATTERN.split(value);
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) { // 允许并发重复创建
            numbers = new ConcurrentHashMap<String, Number>();
        }
        return numbers;
    }

    private Map<String, ConsulURL> getUrls() {
        if (urls == null) { // 允许并发重复创建
            urls = new ConcurrentHashMap<String, ConsulURL>();
        }
        return urls;
    }

    public ConsulURL getUrlParameter(String key) {
        ConsulURL u = getUrls().get(key);
        if (u != null) {
            return u;
        }
        String value = getParameterAndDecoded(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        u = ConsulURL.valueOf(value);
        getUrls().put(key, u);
        return u;
    }

    public double getParameter(String key, double defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.doubleValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        double d = Double.parseDouble(value);
        getNumbers().put(key, d);
        return d;
    }

    public float getParameter(String key, float defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.floatValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(key, f);
        return f;
    }

    public long getParameter(String key, long defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.longValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(key, l);
        return l;
    }

    public int getParameter(String key, int defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    public short getParameter(String key, short defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.shortValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        short s = Short.parseShort(value);
        getNumbers().put(key, s);
        return s;
    }

    public byte getParameter(String key, byte defaultValue) {
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.byteValue();
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        byte b = Byte.parseByte(value);
        getNumbers().put(key, b);
        return b;
    }

    public char getParameter(String key, char defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value.charAt(0);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public boolean isLocalHost() {
        return NetUtils.isLocalHost(host) || getParameter(ConsulConstants.LOCALHOST_KEY, false);
    }

    public boolean isAnyHost() {
        return ConsulConstants.ANYHOST_VALUE.equals(host) || getParameter(ConsulConstants.ANYHOST_KEY, false);
    }

    public ConsulURL addParameterAndEncoded(String key, String value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    public ConsulURL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, Enum<?> value) {
        if (value == null)
            return this;
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, Number value) {
        if (value == null)
            return this;
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0)
            return this;
        return addParameter(key, String.valueOf(value));
    }

    public ConsulURL addParameter(String key, String value) {
        if (key == null || key.length() == 0 || value == null || value.length() == 0) {
            return this;
        }
        // 如果没有修改，直接返回。
        if (value.equals(getParameters().get(key))) { // value != null
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.put(key, value);
        return new ConsulURL(protocol, host, port, path, interfaceId, group, map);
    }

    public ConsulURL addParameterIfAbsent(String key, String value) {
        if (key == null || key.length() == 0 || value == null || value.length() == 0) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.put(key, value);
        return new ConsulURL(protocol, host, port, path, interfaceId, group, map);
    }

    public ConsulURL addParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return this;
        }
        boolean hasAndEqual = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = getParameters().get(entry.getKey());
            if (value == null && entry.getValue() != null || !value.equals(entry.getValue())) {
                hasAndEqual = false;
                break;
            }
        }
        // 如果没有修改，直接返回。
        if (hasAndEqual)
            return this;

        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.putAll(parameters);
        return new ConsulURL(protocol, host, port, path, interfaceId, group, map);
    }

    public ConsulURL addParametersIfAbsent(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(parameters);
        map.putAll(getParameters());
        return new ConsulURL(protocol, host, port, path, interfaceId, group, map);
    }

    public ConsulURL addParameters(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return this;
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map pairs can not be odd number.");
        }
        Map<String, String> map = new HashMap<String, String>();
        int len = pairs.length / 2;
        for (int i = 0; i < len; i++) {
            map.put(pairs[2 * i], pairs[2 * i + 1]);
        }
        return addParameters(map);
    }

    public ConsulURL addParameterString(String query) {
        if (query == null || query.length() == 0) {
            return this;
        }
        return addParameters(ConsulURLUtils.parseQueryString(query));
    }

    public ConsulURL removeParameter(String key) {
        if (key == null || key.length() == 0) {
            return this;
        }
        return removeParameters(key);
    }

    public ConsulURL removeParameters(Collection<String> keys) {
        if (keys == null || keys.size() == 0) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    public ConsulURL removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        for (String key : keys) {
            map.remove(key);
        }
        if (map.size() == getParameters().size()) {
            return this;
        }
        return new ConsulURL(protocol, host, port, path, interfaceId, group, map);
    }

    public ConsulURL clearParameters() {
        return new ConsulURL(protocol, host, port, path, interfaceId, group, new HashMap<String, String>());
    }

    public String getRawParameter(String key) {
        if ("protocol".equals(key))
            return protocol;
        if ("host".equals(key))
            return host;
        if ("port".equals(key))
            return String.valueOf(port);
        if ("path".equals(key))
            return path;
        return getParameter(key);
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<String, String>(parameters);
        if (protocol != null)
            map.put("protocol", protocol);
        if (host != null)
            map.put("host", host);
        if (port > 0)
            map.put("port", String.valueOf(port));
        if (path != null)
            map.put("path", path);
        return map;
    }

    public String toString() {
        if (string != null) {
            return string;
        }
        return string = buildString(false, true); // no show username
    }

    public String toString(String... parameters) {
        return buildString(false, true, parameters); // no show username
    }

    public String toIdentityString() {
        if (identity != null) {
            return identity;
        }
        return identity = buildString(true, false); // only return identity message, see the method "equals" and
        // "hashCode"
    }

    public String toIdentityString(String... parameters) {
        return buildString(true, false, parameters); // only return identity message, see the method "equals" and
        // "hashCode"
    }

    public String toFullString() {
        if (full != null) {
            return full;
        }
        return full = buildString(true, true);
    }

    public static Map<String, String> toStringMap(String... pairs) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (pairs.length > 0) {
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pairs must be even.");
            }
            for (int i = 0; i < pairs.length; i = i + 2) {
                parameters.put(pairs[i], pairs[i + 1]);
            }
        }
        return parameters;
    }

    public String toFullString(String... parameters) {
        return buildString(true, true, parameters);
    }

    public String toParameterString() {
        if (parameter != null) {
            return parameter;
        }
        return parameter = toParameterString(new String[0]);
    }

    public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }

    private void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (getParameters() != null && getParameters().size() > 0) {
            List<String> includes = (parameters == null || parameters.length == 0 ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(getParameters()).entrySet()) {
                if (entry.getKey() != null && entry.getKey().length() > 0
                    && (includes == null || includes.contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append("?");
                        }
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }

    private String buildString(boolean appendUser, boolean appendParameter, String... parameters) {
        return buildString(appendUser, appendParameter, false, false, parameters);
    }

    private String buildString(boolean appendUser, boolean appendParameter, boolean useIP, boolean useService,
                               String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (protocol != null && protocol.length() > 0) {
            buf.append(protocol);
            buf.append("://");
        }
        String host;
        if (useIP) {
            host = getIp();
        } else {
            host = getHost();
        }
        if (host != null && host.length() > 0) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }
        String path;
        if (useService) {
            path = getServiceKey();
        } else {
            path = getPath();
        }
        if (path != null && path.length() > 0) {
            buf.append("/");
            buf.append(path);
        }
        if (appendParameter) {
            buildParameters(buf, true, parameters);
        }
        return buf.toString();
    }

    public URI toJavaURI() {
        try {
            return new URI(toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    public String getServiceKey() {
        String inf = getServiceInterface();
        if (inf == null)
            return null;
        StringBuilder buf = new StringBuilder();
        String group = getGroup();
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(inf);
        String version = getVersion();
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }

    public String getGroup() {
        String group = getParameter(RpcConstants.CONFIG_KEY_UNIQUEID, RpcConstants.ADDRESS_DEFAULT_GROUP);
        return group;
    }

    public String getVersion() {
        String group = getParameter(RpcConstants.CONFIG_KEY_RPC_VERSION, ConsulConstants.DEFAULT_VERSION);
        return group;
    }

    public String getServiceInterface() {
        String interfaceId = getParameter(RpcConstants.CONFIG_KEY_INTERFACE, "");
        if (StringUtils.isEmpty(interfaceId)) {
            interfaceId = getParameter("interfaceId");
        }
        return interfaceId;
    }

    public String toServiceString() {
        return buildString(true, false, true, true);
    }

    public ConsulURL setServiceInterface(String service) {
        return addParameter(RpcConstants.CONFIG_KEY_INTERFACE, service);
    }

    public static String encode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((this.getGroup() == null) ? 0 : this.getGroup().hashCode());
        result = prime * result + ((this.getVersion() == null) ? 0 : this.getVersion().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsulURL other = (ConsulURL) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (port != other.port)
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (this.getGroup() == null) {
            if (other.getGroup() != null)
                return false;
        } else if (!this.getGroup().equals(other.getGroup()))
            return false;
        if (this.getVersion() == null) {
            if (other.getVersion() != null)
                return false;
        } else if (!this.getVersion().equals(other.getVersion()))
            return false;

        return true;
    }

}
