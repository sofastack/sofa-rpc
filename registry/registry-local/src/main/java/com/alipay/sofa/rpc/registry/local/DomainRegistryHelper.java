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
package com.alipay.sofa.rpc.registry.local;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.google.common.net.InetAddresses;

/**
 * @author zhaowang
 * @version : DomainRegistryHelper.java, v 0.1 2022年05月24日 5:26 下午 zhaowang
 */
public class DomainRegistryHelper {

    /**
     * bolt://taobao.com:80?a=b   true
     * taobao.com:80?a=b          true
     *
     * @param url
     * @return
     */
    public static boolean isDomain(String url) {
        String host = getDomain(url);
        if (StringUtils.isEmpty(host)) {
            return false;
        } else {
            return !isIp(host);
        }
    }

    private static boolean isIp(String host) {
        return InetAddresses.isInetAddress(host);
    }

    public static String getDomain(String url) {
        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url);
        return providerInfo.getHost();
    }
}