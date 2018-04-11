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
package com.alipay.sofa.rpc.transmit.ip;

import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.common.struct.ListDifference;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存了配置中心下发的转发 IP 列表
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitAddressHolder {

    private final String                    appName;

    private final ConcurrentHashSet<String> currentUrls = new ConcurrentHashSet<String>();

    public IpTransmitAddressHolder(String appName) {
        this.appName = appName;
    }

    /**
     * full quantity update
     * @param dataId
     * @param urls
     */
    public void processSubscribe(String dataId, List<String> urls) {

        // 先更新最新列表，这样新的转发客户端可以拿到最新列表
        IpTransmitHandler handler = IpTransmitLauncher.getHandler(appName);
        if (handler != null) {
            handler.setAvailableTransmitAddresses(urls);
        }

        // 再通知已有的转发客户端更新地址 TODO app级别？
        List<String> nowUrls = new ArrayList<String>(currentUrls);
        List<String> newUrls = new ArrayList<String>(urls);

        ListDifference<String> diff = new ListDifference<String>(newUrls, nowUrls);
        List<String> needAdd = diff.getOnlyOnLeft(); //需要增加
        List<String> needDelete = diff.getOnlyOnRight(); // 需要删掉

        if (!needAdd.isEmpty()) {
            for (String add : needAdd) {
                IpTransmitClientFactory.addConnectionByIp(add);
            }
        }

        if (!needDelete.isEmpty()) {
            for (String delete : needDelete) {
                IpTransmitClientFactory.removeConnectionByIp(delete);
            }
        }

        // 更新下本地缓存
        currentUrls.addAll(needAdd);
        currentUrls.removeAll(needDelete);
    }

    /**
     * Incremental updating for add
     * @param url
     */
    public void addAvailableAddress(String url) {
        IpTransmitHandler handler = IpTransmitLauncher.getHandler(appName);

        handler.addAvailableTransmitAddresses(url);
        currentUrls.add(url);

    }

    /**
     * Incremental updating for delete
     * @param url
     */
    public void deleteAvailableAddress(String url) {
        IpTransmitHandler handler = IpTransmitLauncher.getHandler(appName);

        handler.deleteAvailableTransmitAddresses(url);
        currentUrls.remove(url);
    }

}