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
package com.alipay.sofa.rpc.bootstrap.bolt;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.bootstrap.DefaultClientProxyInvoker;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_HESSIAN;
import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_HESSIAN2;
import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_JAVA;
import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_PROTOBUF;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltClientProxyInvoker extends DefaultClientProxyInvoker {
    /**
     * 构造执行链
     *
     * @param bootstrap 调用端配置
     */
    public BoltClientProxyInvoker(ConsumerBootstrap bootstrap) {
        super(bootstrap);
    }

    @Override
    protected void cacheCommonData() {
        // 缓存数据
        this.serviceName = ConfigUniqueNameGenerator.getUniqueName(consumerConfig);
        this.serializeType = parseSerializeType(consumerConfig.getSerialization());
    }

    @Override
    protected Byte parseSerializeType(String serialization) {
        Byte serializeType;
        if (SERIALIZE_HESSIAN.equals(serialization)
            || SERIALIZE_HESSIAN2.equals(serialization)) {
            serializeType = RemotingConstants.SERIALIZE_CODE_HESSIAN;
        } else if (SERIALIZE_PROTOBUF.equals(serialization)) {
            serializeType = RemotingConstants.SERIALIZE_CODE_PROTOBUF;
        } else if (SERIALIZE_JAVA.equals(serialization)) {
            serializeType = RemotingConstants.SERIALIZE_CODE_JAVA;
        } else {
            throw new SofaRpcRuntimeException("Unsupported serialization type");
        }
        return serializeType;
    }
}
