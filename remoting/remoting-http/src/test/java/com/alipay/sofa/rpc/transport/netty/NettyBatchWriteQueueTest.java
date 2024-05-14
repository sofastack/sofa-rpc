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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author chengming
 * @version NettyBatchWriteQueueTest.java, v 0.1 2024年03月01日 11:06 AM chengming
 */
public class NettyBatchWriteQueueTest {

    @Mock
    private Channel              mockChannel;

    @Mock
    private EventLoop            mockEventLoop;

    @Mock
    private ChannelPromise       mockChannelPromise;

    private NettyBatchWriteQueue nettyBatchWriteQueue;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockChannel.eventLoop()).thenReturn(mockEventLoop);
        when(mockChannel.newPromise()).thenReturn(mockChannelPromise);
        nettyBatchWriteQueue = NettyBatchWriteQueue.createWriteQueue(mockChannel);
    }

    @Test
    public void testEnqueue() {
        Object message = new Object();
        ChannelFuture future = nettyBatchWriteQueue.enqueue(message);
        Assert.assertNotNull(future);

        Mockito.verify(mockEventLoop).execute(any(Runnable.class));
    }

    @Test
    public void testPrepare() {
        Object message = new Object();
        NettyBatchWriteQueue.MessageTuple messageTuple = new NettyBatchWriteQueue.MessageTuple(message,
            mockChannelPromise);
        nettyBatchWriteQueue.prepare(messageTuple);

        Mockito.verify(mockChannel).write(eq(message), eq(mockChannelPromise));
    }

    @Test
    public void testFlush() {
        Object message = new Object();
        NettyBatchWriteQueue.MessageTuple messageTuple = new NettyBatchWriteQueue.MessageTuple(message,
            mockChannelPromise);
        nettyBatchWriteQueue.flush(messageTuple);

        Mockito.verify(mockChannel).write(eq(message), eq(mockChannelPromise));
        Mockito.verify(mockChannel).flush();
    }
}