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

import com.alipay.sofa.rpc.common.BatchExecutorQueue;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

/**
 * @author chengming
 * @version NettyBatchWriteQueue.java, v 0.1 2024年02月28日 5:42 PM chengming
 */
public class NettyBatchWriteQueue extends BatchExecutorQueue<NettyBatchWriteQueue.MessageTuple> {

    private final Channel   channel;

    private final EventLoop eventLoop;

    private NettyBatchWriteQueue(Channel channel) {
        this.channel = channel;
        this.eventLoop = channel.eventLoop();
    }

    public ChannelFuture enqueue(Object message) {
        return enqueue(message, channel.newPromise());
    }

    public ChannelFuture enqueue(Object message, ChannelPromise channelPromise) {
        MessageTuple messageTuple = new MessageTuple(message, channelPromise);
        super.enqueue(messageTuple, eventLoop);
        return messageTuple.channelPromise;
    }

    @Override
    protected void prepare(MessageTuple item) {
        channel.write(item.originMessage, item.channelPromise);
    }

    @Override
    protected void flush(MessageTuple item) {
        prepare(item);
        channel.flush();
    }

    public static NettyBatchWriteQueue createWriteQueue(Channel channel) {
        return new NettyBatchWriteQueue(channel);
    }

    static class MessageTuple {

        private final Object         originMessage;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object originMessage, ChannelPromise channelPromise) {
            this.originMessage = originMessage;
            this.channelPromise = channelPromise;
        }
    }
}
