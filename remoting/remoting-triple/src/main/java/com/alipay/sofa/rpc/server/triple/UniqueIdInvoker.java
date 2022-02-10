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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * unique id invoker
 *
 * @author huicha
 * @version : UniqueIdInvoker.java 2020/12/24
 */
public class UniqueIdInvoker implements Invoker {

    private ReadWriteLock        rwLock;

    private Lock                 readLock;

    private Lock                 writeLook;

    private Map<String, Invoker> uniqueIdInvokerMap;

    public UniqueIdInvoker() {
        this.rwLock = new ReentrantReadWriteLock();
        this.readLock = this.rwLock.readLock();
        this.writeLook = this.rwLock.writeLock();
        this.uniqueIdInvokerMap = new HashMap<>();
    }

    /**
     * register invoker with unique id from provider config
     *
     * @param providerConfig provider config
     * @param invoker        invoker
     * @return register success or not
     */
    public boolean registerInvoker(ProviderConfig providerConfig, Invoker invoker) {
        this.writeLook.lock();
        try {
            String uniqueId = this.getUniqueId(providerConfig);
            return this.uniqueIdInvokerMap.putIfAbsent(uniqueId, invoker) == null;
        } finally {
            this.writeLook.unlock();
        }
    }

    /**
     * unregister invoker with unique id from provider config
     *
     * @param providerConfig provider config
     * @return unregister success or not
     */
    public boolean unRegisterInvoker(ProviderConfig providerConfig) {
        this.writeLook.lock();
        try {
            String uniqueId = this.getUniqueId(providerConfig);
            return this.uniqueIdInvokerMap.remove(uniqueId) != null;
        } finally {
            this.writeLook.unlock();
        }
    }

    /**
     * returns is there any invoker has registed into this invoker
     */
    public boolean hasInvoker() {
        this.readLock.lock();
        try {
            return !this.uniqueIdInvokerMap.isEmpty();
        } finally {
            this.readLock.unlock();
        }
    }

    /**
     * get unique id from provider config, this function won't return null
     *
     * @param providerConfig provider config
     * @return unique id in provider config
     */
    private String getUniqueId(ProviderConfig providerConfig) {
        String uniqueId = providerConfig.getUniqueId();
        return null == uniqueId ? "" : uniqueId;
    }

    /**
     * get unique id from rpc invoke context
     *
     * @return unique id from rpc invoke context
     */
    private String getUniqueIdFromInvokeContext() {
        String uniqueId = (String) RpcInvokeContext.getContext().get(TripleContants.SOFA_UNIQUE_ID);
        return uniqueId == null ? "" : uniqueId;
    }

    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        // find invoker and invoke
        String interfaceName = request.getInterfaceName();
        String uniqueId = this.getUniqueIdFromInvokeContext();
        Invoker invoker = this.findInvoker(interfaceName, uniqueId);
        return invoker.invoke(request);
    }

    private Invoker findInvoker(String interfaceName, String uniqueId) {
        this.readLock.lock();
        try {
            if (this.uniqueIdInvokerMap.isEmpty()) {
                // invoker map is empty, can not find any invoker
                throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find service of [" +
                    interfaceName + "] with uniqueId [" + uniqueId + "]");
            }

            // try to find invoker
            Invoker invoker = this.uniqueIdInvokerMap.get(uniqueId);
            if (null == invoker) {
                if (StringUtils.isNotEmpty(uniqueId)) {
                    // there has unique id in request, should not fallback.
                    throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find service of [" +
                        interfaceName + "] with uniqueId [" + uniqueId + "]");
                }

                if (this.uniqueIdInvokerMap.size() > 1) {
                    // can not find invoker and there was more than one invoker exists
                    // not sure which one to use, invoke fail.
                    throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find service of [" +
                        interfaceName + "] with uniqueId [" + uniqueId + "]");
                }

                // can not find invoker, but there was only one invoker, use it.
                invoker = this.uniqueIdInvokerMap.values().stream().findFirst().get();
            }

            return invoker;
        } finally {
            this.readLock.unlock();
        }
    }

}
