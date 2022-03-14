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
package com.alipay.sofa.rpc.ext;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>一个可扩展接口类，对应一个加载器</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ExtensionLoader<T> {
    /**
     * slf4j Logger for this class
     */
    private final static Logger                              LOGGER         = LoggerFactory
                                                                                .getLogger(ExtensionLoader.class);
    private static final String                              LOAD_FROM_CODE = "DYNAMIC LOAD EXTENSION BY CODE";

    /**
     * 当前加载的接口类名
     */
    protected final Class<T>                                 interfaceClass;

    /**
     * 接口名字
     */
    protected final String                                   interfaceName;

    /**
     * 扩展点是否单例
     */
    protected final Extensible                               extensible;

    /**
     * 全部的加载的实现类 {"alias":ExtensionClass}
     */
    protected final ConcurrentMap<String, ExtensionClass<T>> all;

    /**
     * 如果是单例，那么factory不为空
     */
    protected final ConcurrentMap<String, T>                 factory;

    /**
     * 加载监听器
     */
    protected final List<ExtensionLoaderListener<T>>         listeners;

    /**
     * 构造函数（自动加载）
     *
     * @param interfaceClass 接口类
     * @param listener       加载后的监听器
     */
    public ExtensionLoader(Class<T> interfaceClass, ExtensionLoaderListener<T> listener) {
        this(interfaceClass, true, listener);
    }

    /**
     * 构造函数（自动加载）
     *
     * @param interfaceClass 接口类
     */
    protected ExtensionLoader(Class<T> interfaceClass) {
        this(interfaceClass, true, null);
    }

    /**
     * 构造函数（主要测试用）
     *
     * @param interfaceClass 接口类
     * @param autoLoad       是否自动开始加载
     * @param listener       扩展加载监听器
     */
    protected ExtensionLoader(Class<T> interfaceClass, boolean autoLoad, ExtensionLoaderListener<T> listener) {
        if (RpcRunningState.isShuttingDown()) {
            this.interfaceClass = null;
            this.interfaceName = null;
            this.listeners = null;
            this.factory = null;
            this.extensible = null;
            this.all = null;
            return;
        }
        // 接口为空，既不是接口，也不是抽象类
        if (interfaceClass == null ||
                !(interfaceClass.isInterface() || Modifier.isAbstract(interfaceClass.getModifiers()))) {
            throw new IllegalArgumentException("Extensible class must be interface or abstract class!");
        }
        this.interfaceClass = interfaceClass;
        this.interfaceName = ClassTypeUtils.getTypeStr(interfaceClass);
        this.listeners = new ArrayList<>();
        if (listener != null) {
            listeners.add(listener);
        }
        Extensible extensible = interfaceClass.getAnnotation(Extensible.class);
        if (extensible == null) {
            throw new IllegalArgumentException(
                    "Error when load extensible interface " + interfaceName + ", must add annotation @Extensible.");
        } else {
            this.extensible = extensible;
        }

        this.factory = extensible.singleton() ? new ConcurrentHashMap<String, T>() : null;
        this.all = new ConcurrentHashMap<String, ExtensionClass<T>>();
        if (autoLoad) {
            List<String> paths = RpcConfigs.getListValue(RpcOptions.EXTENSION_LOAD_PATH);
            for (String path : paths) {
                loadFromFile(path);
            }
        }
    }

    /**
     * @param path path必须以/结尾
     */
    protected synchronized void loadFromFile(String path) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loading extension of extensible {} from path: {}", interfaceName, path);
        }
        // 默认如果不指定文件名字，就是接口名
        String file = StringUtils.isBlank(extensible.file()) ? interfaceName : extensible.file().trim();
        String fullFileName = path + file;
        try {
            ClassLoader classLoader = ClassLoaderUtils.getClassLoader(getClass());
            loadFromClassLoader(classLoader, fullFileName);
        } catch (Throwable t) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to load extension of extensible " + interfaceName + " from path:" + fullFileName,
                    t);
            }
        }
    }

    protected void loadFromClassLoader(ClassLoader classLoader, String fullFileName) throws Throwable {
        Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fullFileName)
            : ClassLoader.getSystemResources(fullFileName);
        // 可能存在多个文件。
        if (urls != null) {
            while (urls.hasMoreElements()) {
                // 读取一个文件
                URL url = urls.nextElement();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Loading extension of extensible {} from classloader: {} and file: {}",
                        interfaceName, classLoader, url);
                }
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        readLine(url, line);
                    }
                } catch (Throwable t) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Failed to load extension of extensible " + interfaceName
                            + " from classloader: " + classLoader + " and file:" + url, t);
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
        }
    }

    protected void readLine(URL url, String line) {
        String[] aliasAndClassName = parseAliasAndClassName(line);
        if (aliasAndClassName == null || aliasAndClassName.length != 2) {
            return;
        }
        String alias = aliasAndClassName[0];
        String className = aliasAndClassName[1];
        // 读取配置的实现类
        Class tmp;
        try {
            tmp = ClassUtils.forName(className, false);
        } catch (Throwable e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Extension {} of extensible {} is disabled, cause by: {}",
                    className, interfaceName, ExceptionUtils.toShortString(e, 2));
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Extension " + className + " of extensible " + interfaceName + " is disabled.", e);
            }
            return;
        }

        loadExtension(alias, tmp, StringUtils.toString(url), className);
    }

    private void loadExtension(String alias, Class loadedClazz, String location, String className) {
        if (!interfaceClass.isAssignableFrom(loadedClazz)) {
            throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName +
                " from file:" + location + ", " + className + " is not subtype of interface.");
        }
        Class<? extends T> implClass = (Class<? extends T>) loadedClazz;

        // 检查是否有可扩展标识
        Extension extension = implClass.getAnnotation(Extension.class);
        if (extension == null) {
            throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName +
                " from file:" + location + ", " + className + " must add annotation @Extension.");
        } else {
            String aliasInCode = extension.value();
            if (StringUtils.isBlank(aliasInCode)) {
                // 扩展实现类未配置@Extension 标签
                throw new IllegalArgumentException("Error when load extension of extensible " + interfaceClass +
                    " from file:" + location + ", " + className + "'s alias of @Extension is blank");
            }
            if (alias == null) {
                // spi文件里没配置，用代码里的
                alias = aliasInCode;
            } else {
                // spi文件里配置的和代码里的不一致
                if (!aliasInCode.equals(alias)) {
                    throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName +
                        " from file:" + location + ", aliases of " + className + " are " +
                        "not equal between " + aliasInCode + "(code) and " + alias + "(file).");
                }
            }
            // 接口需要编号，实现类没设置
            if (extensible.coded() && extension.code() < 0) {
                throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName +
                    " from file:" + location + ", code of @Extension must >=0 at " + className + ".");
            }
        }
        // 不可以是default和*
        if (StringUtils.DEFAULT.equals(alias) || StringUtils.ALL.equals(alias)) {
            throw new IllegalArgumentException("Error when load extension of extensible " + interfaceName +
                " from file:" + location + ", alias of @Extension must not \"default\" and \"*\" at " + className + ".");
        }
        // 检查是否有存在同名的
        ExtensionClass old = all.get(alias);
        ExtensionClass<T> extensionClass = null;
        if (old != null) {
            // 如果当前扩展可以覆盖其它同名扩展
            if (extension.override()) {
                // 如果优先级还没有旧的高，则忽略
                if (extension.order() < old.getOrder()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Extension of extensible {} with alias {} override from {} to {} failure, " +
                            "cause by: order of old extension is higher",
                            interfaceName, alias, old.getClazz(), implClass);
                    }
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Extension of extensible {} with alias {}: {} has been override to {}",
                            interfaceName, alias, old.getClazz(), implClass);
                    }
                    // 如果当前扩展可以覆盖其它同名扩展
                    extensionClass = buildClass(extension, implClass, alias);
                }
            }
            // 如果旧扩展是可覆盖的
            else {
                if (old.isOverride() && old.getOrder() >= extension.order()) {
                    // 如果已加载覆盖扩展，再加载到原始扩展
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Extension of extensible {} with alias {}: {} has been loaded, ignore origin {}",
                            interfaceName, alias, old.getClazz(), implClass);
                    }
                } else {
                    // 如果不能被覆盖，抛出已存在异常
                    throw new IllegalStateException(
                        "Error when load extension of extensible " + interfaceClass + " from file:" + location +
                            ", Duplicate class with same alias: " + alias + ", " + old.getClazz() + " and " + implClass);
                }
            }
        } else {
            extensionClass = buildClass(extension, implClass, alias);
        }
        if (extensionClass != null) {
            // 检查是否有互斥的扩展点
            for (Map.Entry<String, ExtensionClass<T>> entry : all.entrySet()) {
                ExtensionClass existed = entry.getValue();
                if (extensionClass.getOrder() >= existed.getOrder()) {
                    // 新的优先级 >= 老的优先级，检查新的扩展是否排除老的扩展
                    String[] rejection = extensionClass.getRejection();
                    if (CommonUtils.isNotEmpty(rejection)) {
                        for (String rej : rejection) {
                            existed = all.get(rej);
                            if (existed == null || extensionClass.getOrder() < existed.getOrder()) {
                                continue;
                            }
                            ExtensionClass removed = all.remove(rej);
                            if (removed != null) {
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info(
                                        "Extension of extensible {} with alias {}: {} has been reject by new {}",
                                        interfaceName, removed.getAlias(), removed.getClazz(), implClass);
                                }
                            }
                        }
                    }
                } else {
                    String[] rejection = existed.getRejection();
                    if (CommonUtils.isNotEmpty(rejection)) {
                        for (String rej : rejection) {
                            if (rej.equals(extensionClass.getAlias())) {
                                // 被其它扩展排掉
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info(
                                        "Extension of extensible {} with alias {}: {} has been reject by old {}",
                                        interfaceName, alias, implClass, existed.getClazz());
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            loadSuccess(alias, extensionClass);
        }
    }

    private ExtensionClass<T> buildClass(Extension extension, Class<? extends T> implClass, String alias) {
        ExtensionClass<T> extensionClass = new ExtensionClass<T>(implClass, alias);
        extensionClass.setCode(extension.code());
        extensionClass.setSingleton(extensible.singleton());
        extensionClass.setOrder(extension.order());
        extensionClass.setOverride(extension.override());
        extensionClass.setRejection(extension.rejection());
        return extensionClass;
    }

    private void loadSuccess(String alias, ExtensionClass<T> extensionClass) {
        if (listeners != null) {
            for (ExtensionLoaderListener<T> listener : listeners) {
                try {
                    listener.onLoad(extensionClass);
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Error when load extension of extensible " + interfaceClass + " with alias: "
                            + alias + ".", e);
                    }
                }
            }

        }
        all.put(alias, extensionClass);
    }

    protected String[] parseAliasAndClassName(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        line = line.trim();
        int i0 = line.indexOf('#');
        if (i0 == 0 || line.length() == 0) {
            return null; // 当前行是注释 或者 空
        }
        if (i0 > 0) {
            line = line.substring(0, i0).trim();
        }

        String alias = null;
        String className;
        int i = line.indexOf('=');
        if (i > 0) {
            alias = line.substring(0, i).trim(); // 以代码里的为准
            className = line.substring(i + 1).trim();
        } else {
            className = line;
        }
        if (className.length() == 0) {
            return null;
        }
        return new String[] { alias, className };
    }

    /**
     * 返回全部扩展类
     *
     * @return 扩展类对象
     */
    public ConcurrentMap<String, ExtensionClass<T>> getAllExtensions() {
        return all;
    }

    /**
     * 根据服务别名查找扩展类
     *
     * @param alias 扩展别名
     * @return 扩展类对象
     */
    public ExtensionClass<T> getExtensionClass(String alias) {
        return all == null ? null : all.get(alias);
    }

    /**
     * 得到实例
     *
     * @param alias 别名
     * @return 扩展实例（已判断是否单例）
     */
    public T getExtension(String alias) {
        ExtensionClass<T> extensionClass = getExtensionClass(alias);
        if (extensionClass == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_EXTENSION_NOT_FOUND, interfaceName, alias));
        } else {
            if (extensible.singleton() && factory != null) {
                T t = factory.get(alias);
                if (t == null) {
                    synchronized (this) {
                        t = factory.get(alias);
                        if (t == null) {
                            t = extensionClass.getExtInstance();
                            factory.put(alias, t);
                        }
                    }
                }
                return t;
            } else {
                return extensionClass.getExtInstance();
            }
        }
    }

    /**
     * 得到实例
     *
     * @param alias    别名
     * @param argTypes 扩展初始化需要的参数类型
     * @param args     扩展初始化需要的参数
     * @return 扩展实例（已判断是否单例）
     */
    public T getExtension(String alias, Class[] argTypes, Object[] args) {
        ExtensionClass<T> extensionClass = getExtensionClass(alias);
        if (extensionClass == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_EXTENSION_NOT_FOUND, interfaceName, alias));
        } else {
            if (extensible.singleton() && factory != null) {
                T t = factory.get(alias);
                if (t == null) {
                    synchronized (this) {
                        t = factory.get(alias);
                        if (t == null) {
                            t = extensionClass.getExtInstance(argTypes, args);
                            factory.put(alias, t);
                        }
                    }
                }
                return t;
            } else {
                return extensionClass.getExtInstance(argTypes, args);
            }
        }
    }

    public void loadExtension(Class loadedClass) {
        if (loadedClass == null) {
            throw new IllegalArgumentException("Can not load extension of null");
        }
        loadExtension(null, loadedClass, LOAD_FROM_CODE, loadedClass.getName());
    }

    public void addListener(ExtensionLoaderListener<T> listener) {
        synchronized (this) {
            if (!listeners.contains(listener)) {
                this.listeners.add(listener);
                for (ExtensionClass<T> value : all.values()) {
                    try {
                        listener.onLoad(value);
                    } catch (Exception e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Error when notify listener  of extensible " + interfaceClass +
                                " with alias: "
                                + value.getAlias() + ".", e);
                        }
                    }
                }
            }
        }

    }
}
