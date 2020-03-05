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
package com.alipay.sofa.rpc.proxy;

import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * Factory of Proxy SPI
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class ProxyFactory {

    /**
     * 构建代理类实例
     *
     * @param proxyType    代理类型
     * @param clazz        原始类
     * @param proxyInvoker 代码执行的Invoker
     * @param <T>          类型
     * @return 代理类实例
     * @throws Exception
     */
    public static <T> T buildProxy(String proxyType, Class<T> clazz, Invoker proxyInvoker) throws Exception {
        try {
            ExtensionClass<Proxy> ext = ExtensionLoaderFactory.getExtensionLoader(Proxy.class)
                .getExtensionClass(proxyType);
            if (ext == null) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Proxy", proxyType));
            }
            Proxy proxy = ext.getExtInstance();
            return proxy.getProxy(clazz, proxyInvoker);
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Proxy", proxyType), e);
        }
    }

    /**
     * 解析出代理类的Invoker对象
     *
     * @param proxyObject 代理类实现
     * @return Invoker对象
     */
    public static Invoker getInvoker(Object proxyObject, String proxyType) {
        try {
            ExtensionClass<Proxy> ext = ExtensionLoaderFactory.getExtensionLoader(Proxy.class)
                .getExtensionClass(proxyType);
            if (ext == null) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Registry", proxyType));
            }
            Proxy proxy = ext.getExtInstance();
            return proxy.getInvoker(proxyObject);
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Registry", proxyType));
        }
    }
}