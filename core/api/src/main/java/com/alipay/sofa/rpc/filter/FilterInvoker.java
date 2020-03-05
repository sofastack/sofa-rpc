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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.annotation.JustForTest;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

/**
 * <p>过滤器包装的Invoker对象，主要是隔离了filter和service的关系，这样的话filter也可以是单例</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@ThreadSafe
public class FilterInvoker implements Invoker {

    /**
     * 下一层过滤器
     */
    protected Filter                  nextFilter;

    /**
     * 下一层Invoker
     */
    protected FilterInvoker           invoker;

    /**
     * 过滤器所在的接口，可能是provider或者consumer
     */
    protected AbstractInterfaceConfig config;

    /**
     * <B>unmodifiable</B><br>
     * 一些和请求无关的固定的上下文，这些内容从config里加载，和invocation无关<br>
     * 例如是否开启validation配置，方法级是否开启配置。<br>
     * 像请求ip端口这种和invocation有关的上下文不在此map中。
     */
    protected Map<String, Object>     configContext;

    /**
     * 如果无需下一层过滤器
     *
     * @param config 过滤器所在的接口配置
     */
    protected FilterInvoker(AbstractInterfaceConfig config) {
        this.config = config;
        if (config != null) {
            this.configContext = config.getConfigValueCache(false);
        }
    }

    /**
     * 构造函数
     *
     * @param nextFilter 下一层过滤器
     * @param invoker    下一层调用器
     * @param config     过滤器所在的接口配置
     */
    public FilterInvoker(Filter nextFilter, FilterInvoker invoker, AbstractInterfaceConfig config) {
        this.nextFilter = nextFilter;
        this.invoker = invoker;
        this.config = config;
        if (config != null) {
            this.configContext = config.getConfigValueCache(false);
        }
    }

    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        if (nextFilter == null && invoker == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_FILTER,
                LogCodes.getLog(LogCodes.ERROR_NEXT_FILTER_AND_INVOKER_NULL));
        }
        return nextFilter == null ?
            invoker.invoke(request) :
            nextFilter.invoke(invoker, request);
    }

    /**
     * <B>unmodifiable</B><br>
     * 一些和请求无关的固定的上下文，这些内容从config里加载，和invocation无关<br>
     * 例如是否开启validation配置，方法级是否开启配置。<br>
     * 像请求ip端口这种和invocation有关的上下文不在此map中。
     *
     * @return the configContext
     */
    protected Map<String, Object> getConfigContext() {
        return configContext;
    }

    /**
     * 得到接口配置
     *
     * @return 接口配置
     */
    public AbstractInterfaceConfig getConfig() {
        return config;
    }

    /**
     * 得到下一个FilterInvoker
     *
     * @return FilterInvoker
     */
    @JustForTest
    protected FilterInvoker getInvoker() {
        return invoker;
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName   方法名
     * @param paramKey     参数关键字
     * @param defaultValue 默认值
     * @return 都找不到为false boolean method param
     */
    protected boolean getBooleanMethodParam(String methodName, String paramKey, boolean defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        Boolean o = (Boolean) configContext.get(buildMethodKey(methodName, paramKey));
        if (o == null) {
            o = (Boolean) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName   方法名
     * @param paramKey     参数关键字
     * @param defaultValue 默认值
     * @return 都找不到为null string method param
     */
    protected String getStringMethodParam(String methodName, String paramKey, String defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        String o = (String) configContext.get(buildMethodKey(methodName, paramKey));
        if (o == null) {
            o = (String) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName   方法名
     * @param paramKey     参数关键字
     * @param defaultValue 默认值
     * @return 都找不到为defaultValue int method param
     */
    protected int getIntMethodParam(String methodName, String paramKey, int defaultValue) {
        if (CommonUtils.isEmpty(configContext)) {
            return defaultValue;
        }
        Integer o = (Integer) configContext.get(buildMethodKey(methodName, paramKey));
        if (o == null) {
            o = (Integer) configContext.get(paramKey);
            return o == null ? defaultValue : o;
        } else {
            return o;
        }
    }

    /**
     * 取得方法的特殊参数配置
     *
     * @param methodName 方法名
     * @param paramKey   参数关键字
     * @return 都找不到为null method param
     */
    protected Object getMethodParam(String methodName, String paramKey) {
        if (CommonUtils.isEmpty(configContext)) {
            return null;
        }
        Object o = configContext.get(buildMethodKey(methodName, paramKey));
        return o == null ? configContext.get(paramKey) : o;
    }

    /**
     * Buildmkey string.
     *
     * @param methodName the method name
     * @param key        the key
     * @return the string
     */
    private String buildMethodKey(String methodName, String key) {
        return RpcConstants.HIDE_KEY_PREFIX + methodName + RpcConstants.HIDE_KEY_PREFIX + key;
    }
}
