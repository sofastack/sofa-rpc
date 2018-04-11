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

import java.net.InetSocketAddress;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TestChannel extends AbstractChannel {

    InetSocketAddress remote;
    InetSocketAddress local;

    public TestChannel(InetSocketAddress remote, InetSocketAddress local) {
        this.remote = remote;
        this.local = local;
    }

    @Override
    public Object channelContext() {
        return null;
    }

    @Override
    public Object channel() {
        return null;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return remote;
    }

    @Override
    public InetSocketAddress localAddress() {
        return local;
    }

    @Override
    public void writeAndFlush(Object obj) {

    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
