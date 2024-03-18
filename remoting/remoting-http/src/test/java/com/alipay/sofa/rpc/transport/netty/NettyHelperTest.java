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
package com.alipay.sofa.rpc.transport.netty;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import org.junit.Assert;
import org.junit.Test;

import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_USE_IO_URING;

/**
 * @author chengming
 * @version NettyHelperTest.java, v 0.1 2024年03月18日 2:35 PM chengming
 */
public class NettyHelperTest {

    @Test
    public void testEventLoopGroup() {
        System.setProperty("os.name", "linux111");
        System.setProperty(TRANSPORT_USE_IO_URING, "true");

        EventLoopGroup eventLoopGroup = NettyHelper.eventLoopGroup(1, new NamedThreadFactory("test", true));
        Class<? extends SocketChannel> socketChannel = NettyHelper.socketChannel();
        if (IOUring.isAvailable()) {
            Assert.assertTrue(eventLoopGroup instanceof IOUringEventLoopGroup);
            Assert.assertEquals(IOUringSocketChannel.class, socketChannel);
        } else {
            Assert.assertTrue(eventLoopGroup instanceof NioEventLoopGroup);
            Assert.assertEquals(NioSocketChannel.class, socketChannel);
        }
    }
}
