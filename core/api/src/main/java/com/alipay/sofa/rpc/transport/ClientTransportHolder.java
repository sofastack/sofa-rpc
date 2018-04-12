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
package com.alipay.sofa.rpc.transport;

import com.alipay.sofa.rpc.base.Destroyable;

/**
 * Holder of client transport
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public interface ClientTransportHolder extends Destroyable {

    /**
     * 通过配置获取长连接
     *
     * @param config 传输层配置
     * @return 传输层
     */
    ClientTransport getClientTransport(ClientTransportConfig config);

    /**
     * 销毁长连接
     *
     * @param clientTransport   ClientTransport
     * @return need close client transport
     */
    boolean removeClientTransport(ClientTransport clientTransport);

    /**
     * 长连接数量
     *
     * @return size of client transport
     */
    int size();
}
