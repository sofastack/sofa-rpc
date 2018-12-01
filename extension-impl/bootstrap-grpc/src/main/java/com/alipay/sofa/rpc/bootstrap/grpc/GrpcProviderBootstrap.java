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
package com.alipay.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.DefaultProviderBootstrap;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * GRPC provider bootstrap
 *
 * @author LiangEn.LiWei
 * @date 2018.11.20 5:22 PM
 */
@Extension("grpc")
public class GrpcProviderBootstrap<T> extends DefaultProviderBootstrap<T> {

    /**
     * The constructor
     *
     * @param providerConfig provider config
     */
    protected GrpcProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);
    }

    @Override
    protected Class getProxyClass(ProviderConfig providerConfig) {
        try {
            String abstractClass = providerConfig.getInterfaceId();
            if (StringUtils.isNotBlank(abstractClass)) {
                Class proxyClass = ClassUtils.forName(abstractClass);
                if (proxyClass.isInterface()) {
                    throw ExceptionUtils.buildRuntime("GRPC service.AbstractClass",
                        abstractClass, "GRPC service abstractClass must set abstract class, not interface class");
                }
                return proxyClass;
            } else {
                throw ExceptionUtils.buildRuntime("GRPC service.AbstractClass",
                    "null", "abstractClass must be not null");
            }
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(e.getMessage(), e);
        }
    }
}