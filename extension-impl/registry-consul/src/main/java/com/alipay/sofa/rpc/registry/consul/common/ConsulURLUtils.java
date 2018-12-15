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

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.registry.consul.model.ThrallRoleType;

import java.util.regex.Pattern;

/**
 * ConsulURL工具类
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class ConsulURLUtils {

    private static final Pattern ADDRESS_PATTERN =
                                                         Pattern
                                                             .compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private ConsulURLUtils() {

    }

    public static boolean isValidAddress(String address) {
        String[] ipAndHost = StringUtils.split(address, ":");

        return ipAndHost.length == 2 && ADDRESS_PATTERN.matcher(ipAndHost[0]).matches();
    }

    public static String toServiceName(String group) {
        return ConsulConstants.CONSUL_SERVICE_PRE + group;
    }

    private static String toServicePath(ConsulURL url) {
        String name = url.getServiceInterface();
        String group = url.getGroup();
        return group + ConsulConstants.PATH_SEPARATOR + ConsulURL.encode(name);
    }

    public static String toCategoryPathNotIncludeVersion(ConsulURL url, ThrallRoleType roleType) {
        switch (roleType) {
            case CONSUMER:
                return toServicePath(url) + ConsulConstants.PATH_SEPARATOR + ConsulConstants.CONSUMERS_CATEGORY;
            case PROVIDER:
                return toServicePath(url) + ConsulConstants.PATH_SEPARATOR + ConsulConstants.PROVIDERS_CATEGORY;
            default:
                throw new IllegalArgumentException("there is no role type");
        }

    }

    public static String toCategoryPathIncludeVersion(ConsulURL url, ThrallRoleType roleType) {
        switch (roleType) {
            case CONSUMER:
                return toServicePath(url) + ConsulConstants.PATH_SEPARATOR + url.getVersion()
                    + ConsulConstants.PATH_SEPARATOR + ConsulConstants.CONSUMERS_CATEGORY;
            case PROVIDER:
                return toServicePath(url) + ConsulConstants.PATH_SEPARATOR + url.getVersion()
                    + ConsulConstants.PATH_SEPARATOR + ConsulConstants.PROVIDERS_CATEGORY;
            default:
                throw new IllegalArgumentException("there is no role type");
        }

    }

    public static String healthServicePath(ConsulURL url, ThrallRoleType roleType) {
        return toCategoryPathNotIncludeVersion(url, roleType) + ConsulConstants.PATH_SEPARATOR
            + ConsulURL.encode(url.toFullString());
    }

    public static String ephemralNodePath(ConsulURL url, ThrallRoleType roleType) {
        return ConsulConstants.CONSUL_SERVICE_PRE + toCategoryPathIncludeVersion(url, roleType)
            + ConsulConstants.PATH_SEPARATOR + url.getAddress();
    }

}
