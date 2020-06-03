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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils method of ProviderInfo or ProviderGroup
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderHelper {
    /**
     * Compare two provider group, return add list and remove list
     *
     * @param oldGroup old provider group
     * @param newGroup new provider group
     * @param add      provider list need add
     * @param remove   provider list need remove
     */
    public static void compareGroup(ProviderGroup oldGroup, ProviderGroup newGroup,
                                    List<ProviderInfo> add, List<ProviderInfo> remove) {
        final List<ProviderInfo> oldProviders = oldGroup == null ? null : oldGroup.getProviderInfos();
        final List<ProviderInfo> newProviders = newGroup == null ? null : newGroup.getProviderInfos();
        compareProviders(oldProviders, newProviders, add, remove);
    }

    /**
     * Compare two provider list, return add list and remove list
     *
     * @param oldList old Provider list
     * @param newList new provider list
     * @param add     provider list need add
     * @param remove  provider list need remove
     */
    public static void compareProviders(List<ProviderInfo> oldList, List<ProviderInfo> newList,
                                        List<ProviderInfo> add, List<ProviderInfo> remove) {
        // 比较老列表和当前列表
        if (CommonUtils.isEmpty(oldList)) {
            // 空变成非空
            if (CommonUtils.isNotEmpty(newList)) {
                add.addAll(newList);
            }
            // 空到空，忽略
        } else {
            // 非空变成空
            if (CommonUtils.isEmpty(newList)) {
                remove.addAll(oldList);
            } else {
                // 非空变成非空，比较
                if (CommonUtils.isNotEmpty(oldList)) {
                    List<ProviderInfo> tmpList = new ArrayList<ProviderInfo>(newList);
                    // 遍历老的
                    for (ProviderInfo oldProvider : oldList) {
                        if (tmpList.contains(oldProvider)) {
                            tmpList.remove(oldProvider);
                        } else {
                            // 新的没有，老的有，删掉
                            remove.add(oldProvider);
                        }
                    }
                    add.addAll(tmpList);
                }
            }
        }
    }

    /**
     * Compare two provider group list, return add list and remove list
     *
     * @param oldGroups old provider group list
     * @param newGroups new provider group list
     * @param add       provider list need add
     * @param remove    provider list need remove
     */
    public static void compareGroups(List<ProviderGroup> oldGroups, List<ProviderGroup> newGroups,
                                     List<ProviderInfo> add,
                                     List<ProviderInfo> remove) {
        // 比较老列表和当前列表
        if (CommonUtils.isEmpty(oldGroups)) {
            // 空变成非空
            if (CommonUtils.isNotEmpty(newGroups)) {
                for (ProviderGroup newGroup : newGroups) {
                    add.addAll(newGroup.getProviderInfos());
                }
            }
            // 空到空，忽略
        } else {
            // 非空变成空
            if (CommonUtils.isEmpty(newGroups)) {
                for (ProviderGroup oldGroup : oldGroups) {
                    remove.addAll(oldGroup.getProviderInfos());
                }
            } else {
                // 非空变成非空，比较
                if (CommonUtils.isNotEmpty(oldGroups)) {
                    Map<String, List<ProviderInfo>> oldMap = convertToMap(oldGroups);
                    Map<String, List<ProviderInfo>> mapTmp = convertToMap(newGroups);
                    // 遍历新的
                    for (Map.Entry<String, List<ProviderInfo>> oldEntry : oldMap.entrySet()) {
                        String key = oldEntry.getKey();
                        List<ProviderInfo> oldList = oldEntry.getValue();
                        if (mapTmp.containsKey(key)) {
                            // 老的有，新的也有，比较变化的部分
                            final List<ProviderInfo> newList = mapTmp.remove(key);
                            compareProviders(oldList, newList, add, remove);
                            mapTmp.remove(key);
                        } else {
                            // 老的有，新的没有
                            remove.addAll(oldList);
                        }
                    }
                    // 新的有，老的没有
                    for (Map.Entry<String, List<ProviderInfo>> entry : mapTmp.entrySet()) {
                        add.addAll(entry.getValue());
                    }
                }
            }
        }
    }

    private static Map<String, List<ProviderInfo>> convertToMap(List<ProviderGroup> providerGroups) {
        Map<String, List<ProviderInfo>> map = new HashMap<String, List<ProviderInfo>>(providerGroups.size());
        for (ProviderGroup providerGroup : providerGroups) {
            List<ProviderInfo> ps = map.get(providerGroup.getName());
            if (ps == null) {
                ps = new ArrayList<ProviderInfo>();
                map.put(providerGroup.getName(), ps);
            }
            ps.addAll(providerGroup.getProviderInfos());
        }
        return map;
    }

    /**
     * Is empty boolean.
     *
     * @param group the group
     * @return the boolean
     */
    public static boolean isEmpty(ProviderGroup group) {
        return group == null || group.isEmpty();
    }

    /**
     * Write provider info to url string
     *
     * @param providerInfo Provide info
     * @return the string
     */
    public static String toUrl(ProviderInfo providerInfo) {
        String uri = providerInfo.getProtocolType() + "://" + providerInfo.getHost() + ":" + providerInfo.getPort();
        uri += StringUtils.trimToEmpty(providerInfo.getPath());
        StringBuilder sb = new StringBuilder();
        if (providerInfo.getRpcVersion() > 0) {
            sb.append("&").append(ProviderInfoAttrs.ATTR_RPC_VERSION).append("=").append(providerInfo.getRpcVersion());
        }
        if (providerInfo.getSerializationType() != null) {
            sb.append("&").append(ProviderInfoAttrs.ATTR_SERIALIZATION).append("=")
                .append(providerInfo.getSerializationType());
        }
        for (Map.Entry<String, String> entry : providerInfo.getStaticAttrs().entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (sb.length() > 0) {
            uri += sb.replace(0, 1, "?").toString();
        }
        return uri;
    }

    /**
     * Parse url string to ProviderInfo.
     *
     * @param url the url
     * @return ProviderInfo
     */
    public static ProviderInfo toProviderInfo(String url) {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setOriginUrl(url);
        try {
            int protocolIndex = url.indexOf("://");
            String remainUrl;
            if (protocolIndex > -1) {
                String protocol = url.substring(0, protocolIndex).toLowerCase();
                providerInfo.setProtocolType(protocol);
                remainUrl = url.substring(protocolIndex + 3);
            } else { // 默认
                remainUrl = url;
            }

            int addressIndex = remainUrl.indexOf(StringUtils.CONTEXT_SEP);
            String address;
            if (addressIndex > -1) {
                address = remainUrl.substring(0, addressIndex);
                remainUrl = remainUrl.substring(addressIndex);
            } else {
                int itfIndex = remainUrl.indexOf('?');
                if (itfIndex > -1) {
                    address = remainUrl.substring(0, itfIndex);
                    remainUrl = remainUrl.substring(itfIndex);
                } else {
                    address = remainUrl;
                    remainUrl = "";
                }
            }
            String[] ipAndPort = address.split(":", -1); // TODO 不支持ipv6
            providerInfo.setHost(ipAndPort[0]);
            if (ipAndPort.length > 1) {
                providerInfo.setPort(CommonUtils.parseInt(ipAndPort[1], providerInfo.getPort()));
            }

            // 后面可以解析remainUrl得到interface等 /xxx?a=1&b=2
            if (remainUrl.length() > 0) {
                int itfIndex = remainUrl.indexOf('?');
                if (itfIndex > -1) {
                    String itf = remainUrl.substring(0, itfIndex);
                    providerInfo.setPath(itf);
                    // 剩下是params,例如a=1&b=2
                    remainUrl = remainUrl.substring(itfIndex + 1);
                    String[] params = remainUrl.split("&", -1);
                    for (String parm : params) {
                        String[] kvpair = parm.split("=", -1);
                        if (ProviderInfoAttrs.ATTR_WEIGHT.equals(kvpair[0]) && StringUtils.isNotEmpty(kvpair[1])) {
                            int weight = CommonUtils.parseInt(kvpair[1], providerInfo.getWeight());
                            providerInfo.setWeight(weight);
                            providerInfo.setStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT, String.valueOf(weight));
                        } else if (ProviderInfoAttrs.ATTR_RPC_VERSION.equals(kvpair[0]) &&
                            StringUtils.isNotEmpty(kvpair[1])) {
                            providerInfo.setRpcVersion(CommonUtils.parseInt(kvpair[1], providerInfo.getRpcVersion()));
                        } else if (ProviderInfoAttrs.ATTR_SERIALIZATION.equals(kvpair[0]) &&
                            StringUtils.isNotEmpty(kvpair[1])) {
                            providerInfo.setSerializationType(kvpair[1]);
                        } else {
                            providerInfo.getStaticAttrs().put(kvpair[0], kvpair[1]);
                        }

                    }
                } else {
                    providerInfo.setPath(remainUrl);
                }
            } else {
                providerInfo.setPath(StringUtils.EMPTY);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(LogCodes.getLog(LogCodes.ERROR_CONVERT_URL, url), e);
        }
        return providerInfo;
    }
}
