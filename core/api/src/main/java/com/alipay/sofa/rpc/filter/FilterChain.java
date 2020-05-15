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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.common.struct.OrderedComparator;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoader;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderListener;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chain of filter.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class FilterChain implements Invoker {

    /**
     * 日志
     */
    private static final Logger                              LOGGER                = LoggerFactory
                                                                                       .getLogger(FilterChain.class);

    /**
     * 服务端自动激活的 {"alias":ExtensionClass}
     */
    private final static Map<String, ExtensionClass<Filter>> PROVIDER_AUTO_ACTIVES = Collections
                                                                                       .synchronizedMap(new LinkedHashMap<String, ExtensionClass<Filter>>());

    /**
     * 调用端自动激活的 {"alias":ExtensionClass}
     */
    private final static Map<String, ExtensionClass<Filter>> CONSUMER_AUTO_ACTIVES = Collections
                                                                                       .synchronizedMap(new LinkedHashMap<String, ExtensionClass<Filter>>());

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Filter>             EXTENSION_LOADER      = buildLoader();

    private static ExtensionLoader<Filter> buildLoader() {
        ExtensionLoader<Filter> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        extensionLoader.addListener(new ExtensionLoaderListener<Filter>() {
            @Override
            public void onLoad(ExtensionClass<Filter> extensionClass) {
                Class<? extends Filter> implClass = extensionClass.getClazz();
                // 读取自动加载的类列表。
                AutoActive autoActive = implClass.getAnnotation(AutoActive.class);
                if (autoActive != null) {
                    String alias = extensionClass.getAlias();
                    if (autoActive.providerSide()) {
                        PROVIDER_AUTO_ACTIVES.put(alias, extensionClass);
                    }
                    if (autoActive.consumerSide()) {
                        CONSUMER_AUTO_ACTIVES.put(alias, extensionClass);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Extension of interface " + Filter.class
                            + ", " + implClass + "(" + alias + ") will auto active");
                    }
                }
            }
        });
        return extensionLoader;
    }

    /**
     * 调用链
     */
    private FilterInvoker invokerChain;

    /**
     * 过滤器列表，从底至上排序
     */
    private List<Filter>  loadedFilters;

    /**
     * 构造执行链
     *
     * @param filters     包装过滤器列表
     * @param lastInvoker 最终过滤器
     * @param config      接口配置
     */
    protected FilterChain(List<Filter> filters, FilterInvoker lastInvoker, AbstractInterfaceConfig config) {
        // 调用过程外面包装多层自定义filter
        // 前面的过滤器在最外层
        invokerChain = lastInvoker;
        if (CommonUtils.isNotEmpty(filters)) {
            loadedFilters = new ArrayList<Filter>();
            for (int i = filters.size() - 1; i >= 0; i--) {
                try {
                    Filter filter = filters.get(i);
                    if (filter.needToLoad(invokerChain)) {
                        invokerChain = new FilterInvoker(filter, invokerChain, config);
                        // cache this for filter when async respond
                        loadedFilters.add(filter);
                    }
                } catch (SofaRpcRuntimeException e) {
                    LOGGER.error(LogCodes.getLog(LogCodes.ERROR_FILTER_CONSTRUCT), e);
                    throw e;
                } catch (Exception e) {
                    LOGGER.error(LogCodes.getLog(LogCodes.ERROR_FILTER_CONSTRUCT), e);
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_FILTER_CONSTRUCT), e);
                }
            }
        }
    }

    /**
     * 构造服务端的执行链
     *
     * @param providerConfig provider配置
     * @param lastFilter     最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildProviderChain(ProviderConfig<?> providerConfig, FilterInvoker lastFilter) {
        return new FilterChain(selectActualFilters(providerConfig, PROVIDER_AUTO_ACTIVES), lastFilter, providerConfig);
    }

    /**
     * 构造调用端的执行链
     *
     * @param consumerConfig consumer配置
     * @param lastFilter     最后一个filter
     * @return filter执行链
     */
    public static FilterChain buildConsumerChain(ConsumerConfig<?> consumerConfig, FilterInvoker lastFilter) {
        return new FilterChain(selectActualFilters(consumerConfig, CONSUMER_AUTO_ACTIVES), lastFilter, consumerConfig);
    }

    /**
     * 获取真正的过滤器列表
     *
     * @param config            provider配置或者consumer配置
     * @param autoActiveFilters 系统自动激活的过滤器映射
     * @return 真正的过滤器列表
     */
    private static List<Filter> selectActualFilters(AbstractInterfaceConfig config,
                                                    Map<String, ExtensionClass<Filter>> autoActiveFilters) {
        /*
         * 例如自动装载扩展 A(a),B(b),C(c)  filter=[-a,d]  filterRef=[new E, new Exclude(b)]
         * 逻辑如下：
         * 1.解析config.getFilterRef()，记录E和-b
         * 2.解析config.getFilter()字符串，记录 d 和 -a,-b
         * 3.再解析自动装载扩展，a,b被排除了，所以拿到c,d
         * 4.对c d进行排序
         * 5.拿到C、D实现类
         * 6.加上自定义，返回C、D、E
         */
        // 用户通过自己new实例的方式注入的filter，优先级高
        List<Filter> customFilters = config.getFilterRef() == null ?
            new ArrayList<Filter>() : new CopyOnWriteArrayList<Filter>(config.getFilterRef());
        // 先解析是否有特殊处理
        HashSet<String> excludes = parseExcludeFilter(customFilters);

        // 准备数据：用户通过别名的方式注入的filter，需要解析
        List<ExtensionClass<Filter>> extensionFilters = new ArrayList<ExtensionClass<Filter>>();
        List<String> filterAliases = config.getFilter(); //
        if (CommonUtils.isNotEmpty(filterAliases)) {
            for (String filterAlias : filterAliases) {
                if (startsWithExcludePrefix(filterAlias)) { // 排除用的特殊字符
                    excludes.add(filterAlias.substring(1));
                } else {
                    ExtensionClass<Filter> filter = EXTENSION_LOADER.getExtensionClass(filterAlias);
                    if (filter != null) {
                        extensionFilters.add(filter);
                    }
                }
            }
        }
        // 解析自动加载的过滤器
        if (!excludes.contains(StringUtils.ALL) && !excludes.contains(StringUtils.DEFAULT)) { // 配了-*和-default表示不加载内置
            for (Map.Entry<String, ExtensionClass<Filter>> entry : autoActiveFilters.entrySet()) {
                if (!excludes.contains(entry.getKey())) {
                    extensionFilters.add(entry.getValue());
                }
            }
        }
        // 按order从小到大排序
        if (extensionFilters.size() > 1) {
            Collections.sort(extensionFilters, new OrderedComparator<ExtensionClass<Filter>>());
        }
        List<Filter> actualFilters = new ArrayList<Filter>();
        for (ExtensionClass<Filter> extensionFilter : extensionFilters) {
            actualFilters.add(extensionFilter.getExtInstance());
        }
        // 加入自定义的过滤器
        actualFilters.addAll(customFilters);
        return actualFilters;
    }

    /**
     * 判断是否需要排除自定义过滤器
     *
     * @param customFilters 自定义filter列表
     * @return 需要排除的过滤器的key列表
     */
    private static HashSet<String> parseExcludeFilter(List<Filter> customFilters) {
        HashSet<String> excludeKeys = new HashSet<String>();
        if (CommonUtils.isNotEmpty(customFilters)) {
            for (Filter filter : customFilters) {
                if (filter instanceof ExcludeFilter) {
                    // 存在需要排除的过滤器
                    ExcludeFilter excludeFilter = (ExcludeFilter) filter;
                    String excludeName = excludeFilter.getExcludeName();
                    if (StringUtils.isNotEmpty(excludeName)) {
                        String excludeFilterName = startsWithExcludePrefix(excludeName) ?
                            excludeName.substring(1)
                            : excludeName;
                        if (StringUtils.isNotEmpty(excludeFilterName)) {
                            excludeKeys.add(excludeFilterName);
                        }
                    }
                    customFilters.remove(filter);
                }
            }
        }
        if (!excludeKeys.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Find exclude filters: {}", excludeKeys);
            }
        }
        return excludeKeys;
    }

    private static boolean startsWithExcludePrefix(String excludeName) {
        char c = excludeName.charAt(0);
        return c == '-' || c == '!';
    }

    @Override
    public SofaResponse invoke(SofaRequest sofaRequest) throws SofaRpcException {
        return invokerChain.invoke(sofaRequest);
    }

    /**
     * Do filtering when async respond from server
     *
     * @param config    Consumer config
     * @param request   SofaRequest
     * @param response  SofaResponse
     * @param throwable Throwable when invoke
     * @throws SofaRpcException occur error
     */
    public void onAsyncResponse(ConsumerConfig config, SofaRequest request, SofaResponse response, Throwable throwable)
        throws SofaRpcException {
        try {
            for (Filter loadedFilter : loadedFilters) {
                loadedFilter.onAsyncResponse(config, request, response, throwable);
            }
        } catch (SofaRpcException e) {
            LOGGER
                .errorWithApp(config.getAppName(), "Catch exception when do filtering after asynchronous respond.", e);
        }
    }

    /**
     * 得到执行链
     *
     * @return chain
     */
    protected Invoker getChain() {
        return invokerChain;
    }

}
