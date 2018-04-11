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

import com.alipay.sofa.rpc.context.RpcRuntimeContext;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认配置带ID
 *
 * @param <S> the sub class of AbstractIdConfig
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public abstract class AbstractIdConfig<S extends AbstractIdConfig> implements Serializable {

    private static final long          serialVersionUID = -1932911135229369183L;

    /**
     * Id生成器
     */
    private final static AtomicInteger ID_GENERATOR     = new AtomicInteger(0);

    static {
        RpcRuntimeContext.now();
    }

    /**
     * config id 
     */
    private String                     id;

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        if (id == null) {
            synchronized (this) {
                if (id == null) {
                    id = "rpc-cfg-" + ID_GENERATOR.getAndIncrement();
                }
            }
        }
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public S setId(String id) {
        this.id = id;
        return castThis();
    }

    protected S castThis() {
        return (S) this;
    }
}
