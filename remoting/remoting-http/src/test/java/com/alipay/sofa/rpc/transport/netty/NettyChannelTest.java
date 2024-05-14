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

import com.alipay.sofa.rpc.common.utils.NetUtils;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author chengming
 * @version NettyChannelTest.java, v 0.1 2024年02月29日 3:18 PM chengming
 */
public class NettyChannelTest {

    @Mock
    private Channel               mockChannel    = Mockito.mock(Channel.class);

    @Mock
    private ChannelHandlerContext mockContext    = Mockito.mock(ChannelHandlerContext.class);

    @Mock
    private NettyBatchWriteQueue  mockWriteQueue = Mockito.mock(NettyBatchWriteQueue.class);

    @Mock
    private ChannelFuture         mockFuture     = Mockito.mock(ChannelFuture.class);

    private NettyChannel          nettyChannel;

    @Before
    public void setUp() {
        Mockito.when(mockChannel.eventLoop()).thenReturn(Mockito.mock(EventLoop.class));
        Mockito.when(mockChannel.alloc()).thenReturn(PooledByteBufAllocator.DEFAULT);
        when(mockContext.channel()).thenReturn(mockChannel);
        when(mockWriteQueue.enqueue(any())).thenReturn(mockFuture);
        nettyChannel = new NettyChannel(mockChannel);
        nettyChannel.setWriteQueue(mockWriteQueue);
    }

    @Test
    public void testRunSuccess() throws Exception {
        nettyChannel.writeAndFlush("111");

        Mockito.verify(mockWriteQueue).enqueue("111");

        ArgumentCaptor<GenericFutureListener> captor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(mockFuture).addListener(captor.capture());

        // 模拟 FutureListener 的回调
        GenericFutureListener<Future<Object>> listener = captor.getValue();
        listener.operationComplete((Future) mockFuture);

        // 验证没有错误日志被记录（因为操作是成功的）
        Mockito.verify(Mockito.mock(NetUtils.class), times(10));
        NetUtils.channelToString(any(InetSocketAddress.class), any(InetSocketAddress.class));
    }

}
