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
package com.alipay.sofa.rpc.protocol;

import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoader;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory of TelnetHandler
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public class TelnetHandlerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                         LOGGER           = LoggerFactory
                                                                             .getLogger(TelnetHandlerFactory.class);

    /**
     * 保存支持的全部命令，{命令：解析器}
     */
    private static Map<String, TelnetHandler>           supportedCmds    = new ConcurrentHashMap<String, TelnetHandler>();

    /**
     * 扩展器
     */
    private final static ExtensionLoader<TelnetHandler> EXTENSION_LOADER = buildLoader();

    private static ExtensionLoader<TelnetHandler> buildLoader() {
        ExtensionLoader<TelnetHandler> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(TelnetHandler.class);
        extensionLoader.addListener(new ExtensionLoaderListener<TelnetHandler>() {
            @Override
            public void onLoad(ExtensionClass<TelnetHandler> extensionClass) {
                // 自己维护支持列表，不托管给ExtensionLoaderFactory
                TelnetHandler handler = extensionClass.getExtInstance();
                supportedCmds.put(handler.getCommand(), handler);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add telnet handler {}:{}.", handler.getCommand(), handler);
                }
            }
        });
        return extensionLoader;
    }

    public static TelnetHandler getHandler(String command) {
        return supportedCmds.get(command);
    }

    public static Map<String, TelnetHandler> getAllHandlers() {
        return supportedCmds;
    }
}