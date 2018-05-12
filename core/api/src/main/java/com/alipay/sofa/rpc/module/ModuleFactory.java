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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoader;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory of module
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.2.0
 */
public class ModuleFactory {

    /**
     * logger for this class
     */
    private static final Logger                    LOGGER            = LoggerFactory.getLogger(ModuleFactory.class);

    /**
     * 已加载的模块
     */
    static final ConcurrentHashMap<String, Module> INSTALLED_MODULES = new ConcurrentHashMap<String, Module>();

    /**
     * parse module load config
     *
     * @param moduleLoadList alias of all config modules
     * @param moduleName     the module name
     * @return need load
     */
    static boolean needLoad(String moduleLoadList, String moduleName) {
        String[] activatedModules = StringUtils.splitWithCommaOrSemicolon(moduleLoadList);
        boolean match = false;
        for (String activatedModule : activatedModules) {
            if (StringUtils.ALL.equals(activatedModule)) {
                match = true;
            } else if (activatedModule.equals(moduleName)) {
                match = true;
            } else if (match && (activatedModule.equals("!" + moduleName)
                || activatedModule.equals("-" + moduleName))) {
                match = false;
                break;
            }
        }
        return match;
    }

    /**
     * 加载全部模块
     */
    public static void installModules() {
        ExtensionLoader<Module> loader = ExtensionLoaderFactory.getExtensionLoader(Module.class);
        String moduleLoadList = RpcConfigs.getStringValue(RpcOptions.MODULE_LOAD_LIST);
        for (Map.Entry<String, ExtensionClass<Module>> o : loader.getAllExtensions().entrySet()) {
            String moduleName = o.getKey();
            Module module = o.getValue().getExtInstance();
            // judge need load from rpc option
            if (needLoad(moduleLoadList, moduleName)) {
                // judge need load from implement
                if (module.needLoad()) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Install Module: {}", moduleName);
                    }
                    module.install();
                    INSTALLED_MODULES.put(moduleName, module);
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("The module " + moduleName + " does not need to be loaded.");
                    }
                }
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("The module " + moduleName + " is not in the module load list.");
                }
            }
        }
    }

    /**
     * 卸载全部模块
     */
    public static void uninstallModules() {
        for (Map.Entry<String, Module> o : INSTALLED_MODULES.entrySet()) {
            String moduleName = o.getKey();
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Uninstall Module: {}", moduleName);
                }
                o.getValue().uninstall();
                INSTALLED_MODULES.remove(moduleName);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Error when uninstall module " + moduleName, e);
                }
            }
        }
    }

    /**
     * 卸载模块
     *
     * @param moduleName module name
     */
    public static void uninstallModule(String moduleName) {
        Module module = INSTALLED_MODULES.get(moduleName);
        if (module != null) {
            try {
                module.uninstall();
                INSTALLED_MODULES.remove(moduleName);
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Error when uninstall module " + moduleName, e);
                }
            }
        }
    }
}
