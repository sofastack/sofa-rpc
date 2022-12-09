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
package com.alipay.sofa.rpc.bootstrap.triple;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.bootstrap.DefaultClientProxyInvoker;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;

/**
 * @author Even
 * @date 2022/11/17 10:08 PM
 */
public class TripleClientProxyInvoker extends DefaultClientProxyInvoker {

    /**
     * 构造执行链
     *
     * @param bootstrap 调用端配置
     */
    public TripleClientProxyInvoker(ConsumerBootstrap bootstrap) {
        super(bootstrap);
    }

    @Override
    protected void cacheCommonData() {
        // 缓存数据
        this.serviceName = ConfigUniqueNameGenerator.getUniqueName(consumerConfig);
        this.serializeType = parseSerializeType(consumerConfig.getSerialization());
    }

}
