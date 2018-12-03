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
package com.alipay.sofa.rpc.registry;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;

import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("mocktestslow")
public class MockTestSlowRegistry extends MockTestRegistry {

    private int sleepMillis = 2000;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected MockTestSlowRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    protected List<ProviderGroup> doReturn(final ProviderInfoListener listener, final List<ProviderGroup> groups) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                }
                listener.updateAllProviders(groups);
            }
        }, "mock-slow-registry-callback");
        thread.start();
        return null;
    }

    protected ProviderGroup buildProviderGroup() {
        return new ProviderGroup("mocktestslow");
    }
}
