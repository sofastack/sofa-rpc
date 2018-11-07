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
package com.alipay.sofa.rpc.codec.sofahessian.serialize;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;

import java.util.Map;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public interface CustomHessianSerializer<T> {

    /**
     * 反序列化，已有数据，填充字段
     *
     * @param data     原始字节数组
     * @param template 模板对象
     * @param context  上下文
     * @throws SofaRpcException 序列化异常
     */
    public void decodeObjectByTemplate(AbstractByteBuf data, Map<String, String> context,
                                       T template) throws SofaRpcException;

    /**
     * 反序列化，只有类型，返回对象
     *
     * @param data    原始字节数组
     * @param context 上下文
     * @return 反序列化后的对象
     * @throws SofaRpcException 序列化异常
     */
    public T decodeObject(AbstractByteBuf data, Map<String, String> context) throws SofaRpcException;

    /**
     * 序列化
     *
     * @param object  对象
     * @param context 上下文
     * @return 序列化后的对象
     * @throws SofaRpcException 序列化异常
     */
    public AbstractByteBuf encodeObject(T object, Map<String, String> context);
}