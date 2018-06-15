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
package com.alipay.sofa.rpc.config;

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法级配置
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class MethodConfig implements Serializable {

    private static final long      serialVersionUID = -8594337650648536897L;

    /*-------------配置项开始----------------*/
    /**
     * 方法名称，无法做到重载方法的配置
     */
    private String                 name;

    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String>  parameters;

    /**
     * The Timeout. 远程调用超时时间(毫秒)
     */
    protected Integer              timeout;

    /**
     * The Retries. 失败后重试次数
     */
    protected Integer              retries;

    /**
     * 调用方式
     */
    protected String               invokeType;

    /**
     * The Validation. 是否jsr303验证
     */
    protected Boolean              validation;

    /**
     * 返回值之前的listener
     */
    protected SofaResponseCallback onReturn;

    /**
     * 最大并发执行（不管服务端还是客户端）
     */
    protected Integer              concurrents;

    /**
     * 是否启用客户端缓存
     */
    protected Boolean              cache;

    /**
     * 是否启动压缩
     */
    protected String               compress;

    /**
     * 目标参数（机房/分组）索引，第一个参数从0开始
     * // TODO 待实现
     */
    protected Integer              dstParam;

    /*-------------配置项结束----------------*/

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public MethodConfig setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     */
    public MethodConfig setParameters(Map<String, String> parameters) {
        if (this.parameters == null) {
            this.parameters = new ConcurrentHashMap<String, String>();
            this.parameters.putAll(parameters);
        }
        return this;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public MethodConfig setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Gets retries.
     *
     * @return the retries
     */
    public Integer getRetries() {
        return retries;
    }

    /**
     * Sets retries.
     *
     * @param retries the retries
     */
    public MethodConfig setRetries(Integer retries) {
        this.retries = retries;
        return this;
    }

    /**
     * Gets invoke type.
     *
     * @return the invoke type
     */
    public String getInvokeType() {
        return invokeType;
    }

    /**
     * Sets invoke type.
     *
     * @param invokeType the invoke type
     * @return the invoke type
     */
    public MethodConfig setInvokeType(String invokeType) {
        this.invokeType = invokeType;
        return this;
    }

    /**
     * Gets concurrents.
     *
     * @return the concurrents
     */
    public Integer getConcurrents() {
        return concurrents;
    }

    /**
     * Sets concurrents.
     *
     * @param concurrents the concurrents
     */
    public MethodConfig setConcurrents(Integer concurrents) {
        this.concurrents = concurrents;
        return this;
    }

    /**
     * Gets cache.
     *
     * @return the cache
     */
    public Boolean getCache() {
        return cache;
    }

    /**
     * Sets cache.
     *
     * @param cache the cache
     */
    public MethodConfig setCache(Boolean cache) {
        this.cache = cache;
        return this;
    }

    /**
     * Sets validation.
     *
     * @param validation the validation
     */
    public MethodConfig setValidation(Boolean validation) {
        this.validation = validation;
        return this;
    }

    /**
     * Gets validation.
     *
     * @return the validation
     */
    public Boolean getValidation() {
        return validation;
    }

    /**
     * Gets onReturn.
     *
     * @return the onReturn
     */
    public SofaResponseCallback getOnReturn() {
        return onReturn;
    }

    /**
     * Sets onReturn.
     *
     * @param onReturn the onReturn
     */
    public MethodConfig setOnReturn(SofaResponseCallback onReturn) {
        this.onReturn = onReturn;
        return this;
    }

    /**
     * Gets compress.
     *
     * @return the compress
     */
    public String getCompress() {
        return compress;
    }

    /**
     * Sets compress.
     *
     * @param compress the compress
     */
    public MethodConfig setCompress(String compress) {
        this.compress = compress;
        return this;
    }

    /**
     * Gets dst param.
     *
     * @return the dst param
     */
    public Integer getDstParam() {
        return dstParam;
    }

    /**
     * Sets dst param.
     *
     * @param dstParam the dst param
     */
    public MethodConfig setDstParam(Integer dstParam) {
        this.dstParam = dstParam;
        return this;
    }

    /**
     * Sets parameter.
     *
     * @param key   the key
     * @param value the value
     */
    public MethodConfig setParameter(String key, String value) {
        if (parameters == null) {
            parameters = new ConcurrentHashMap<String, String>();
        }
        parameters.put(key, value);
        return this;
    }

    /**
     * Gets parameter.
     *
     * @param key the key
     * @return the value
     */
    public String getParameter(String key) {
        return parameters == null ? null : parameters.get(key);
    }
}
