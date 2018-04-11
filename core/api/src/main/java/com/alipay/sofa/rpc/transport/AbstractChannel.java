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

import com.alipay.sofa.rpc.common.annotation.Unstable;

import java.net.InetSocketAddress;

/**
 * AbstractChannel
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public abstract class AbstractChannel<CONTEXT, CHANNEL> {

    /**
     * 长连接上下文
     *
     * @return ChannelContext
     */
    public abstract CONTEXT channelContext();

    /**
     * 通道
     *
     * @return Channel
     */
    public abstract CHANNEL channel();

    /**
     * 得到连接的远端地址
     *
     * @return the remote address
     */
    public abstract InetSocketAddress remoteAddress();

    /**
     * 得到连接的本地地址（如果是短连接，可能不准）
     *
     * @return the local address
     */
    public abstract InetSocketAddress localAddress();

    /**
     * 写入数据
     *
     * @param obj data which need to write
     */
    public abstract void writeAndFlush(Object obj);

    /**
     * 是否可用
     *
     * @return 是否可以
     */
    public abstract boolean isAvailable();
}
