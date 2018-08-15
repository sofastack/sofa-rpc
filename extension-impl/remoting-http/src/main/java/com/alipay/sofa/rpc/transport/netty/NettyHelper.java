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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.alipay.sofa.rpc.transport.ServerTransportConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_CLIENT_IO_THREADS;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_USE_EPOLL;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class NettyHelper {

    /**
     * Logger for NettyHelper
     **/
    private static final Logger                                 LOGGER           = LoggerFactory
                                                                                     .getLogger(NettyHelper.class);

    /**
     * 服务端Boss线程池（一种协议一个）
     */
    private static ConcurrentMap<String, EventLoopGroup>        serverBossGroups = new ConcurrentHashMap<String, EventLoopGroup>();
    /**
     * 服务端IO线程池（一种协议一个）
     */
    private static ConcurrentMap<String, EventLoopGroup>        serverIoGroups   = new ConcurrentHashMap<String, EventLoopGroup>();

    /**
     * 由于线程池是公用的，需要计数器，在最后一个人关闭时才能销毁
     */
    private static ConcurrentMap<EventLoopGroup, AtomicInteger> refCounter       = new ConcurrentHashMap<EventLoopGroup, AtomicInteger>();

    /**
     * 得到服务端Boss线程池
     *
     * @param config 服务端配置
     * @return 服务端Boss线程池
     */
    public static EventLoopGroup getServerBossEventLoopGroup(ServerTransportConfig config) {
        String type = config.getProtocolType();
        EventLoopGroup bossGroup = serverBossGroups.get(type);
        if (bossGroup == null) {
            synchronized (NettyHelper.class) {
                bossGroup = serverBossGroups.get(config.getProtocolType());
                if (bossGroup == null) {
                    int bossThreads = config.getBossThreads();
                    bossThreads = bossThreads <= 0 ? Math.max(4, SystemInfo.getCpuCores() / 2) : bossThreads;
                    NamedThreadFactory threadName =
                            new NamedThreadFactory("SEV-BOSS-" + config.getPort(), config.isDaemon());
                    bossGroup = config.isUseEpoll() ?
                        new EpollEventLoopGroup(bossThreads, threadName) :
                        new NioEventLoopGroup(bossThreads, threadName);
                    serverBossGroups.put(type, bossGroup);
                    refCounter.putIfAbsent(bossGroup, new AtomicInteger(0));
                }
            }
        }
        refCounter.get(bossGroup).incrementAndGet();
        return bossGroup;
    }

    /**
     * 关闭服务端Boss线程（只有最后一个使用者关闭才生效）
     *
     * @param config 服务端配置
     */
    public static void closeServerBossEventLoopGroup(ServerTransportConfig config) {
        EventLoopGroup bossGroup = serverBossGroups.get(config.getProtocolType());
        if (closeEventLoopGroupIfNoRef(bossGroup)) {
            serverBossGroups.remove(config.getProtocolType());
        }
    }

    /**
     * 得到服务端IO线程池
     *
     * @param config 服务端配置
     * @return 服务端Boss线程池
     */
    public static EventLoopGroup getServerIoEventLoopGroup(ServerTransportConfig config) {
        String type = config.getProtocolType();
        EventLoopGroup ioGroup = serverIoGroups.get(type);
        if (ioGroup == null) {
            synchronized (NettyHelper.class) {
                ioGroup = serverIoGroups.get(config.getProtocolType());
                if (ioGroup == null) {
                    int ioThreads = config.getIoThreads();
                    ioThreads = ioThreads <= 0 ? Math.max(8, SystemInfo.getCpuCores() + 1) : ioThreads;
                    NamedThreadFactory threadName =
                            new NamedThreadFactory("SEV-IO-" + config.getPort(), config.isDaemon());
                    ioGroup = config.isUseEpoll() ?
                        new EpollEventLoopGroup(ioThreads, threadName) :
                        new NioEventLoopGroup(ioThreads, threadName);
                    serverIoGroups.put(type, ioGroup);
                    refCounter.putIfAbsent(ioGroup, new AtomicInteger(0));
                }
            }
        }
        refCounter.get(ioGroup).incrementAndGet();
        return ioGroup;
    }

    /**
     * 关闭服务端IO线程（只有最后一个使用者关闭才生效）
     *
     * @param config 服务端配置
     */
    public static void closeServerIoEventLoopGroup(ServerTransportConfig config) {
        EventLoopGroup ioGroup = serverIoGroups.get(config.getProtocolType());
        if (closeEventLoopGroupIfNoRef(ioGroup)) {
            serverIoGroups.remove(config.getProtocolType());
        }
    }

    /**
     * 如果是这个线程是最后一个使用者，直接删除
     *
     * @param eventLoopGroup 线程池
     * @return 是否被删除
     */
    private static boolean closeEventLoopGroupIfNoRef(EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup != null && refCounter.get(eventLoopGroup).decrementAndGet() <= 0) {
            if (!eventLoopGroup.isShuttingDown() && !eventLoopGroup.isShutdown()) {
                eventLoopGroup.shutdownGracefully();
            }
            refCounter.remove(eventLoopGroup);
            return true;
        }
        return false;
    }

    /**
     * 客户端IO线程 全局共用
     */
    private volatile static EventLoopGroup clientIOEventLoopGroup;

    /**
     * 获取客户端IO线程池
     *
     * @return 客户端IO线程池
     */
    public synchronized static EventLoopGroup getClientIOEventLoopGroup() {
        if (clientIOEventLoopGroup != null && clientIOEventLoopGroup.isShutdown()) {
            clientIOEventLoopGroup = null;
        }
        if (clientIOEventLoopGroup == null) {
            int clientIoThreads = getIntValue(TRANSPORT_CLIENT_IO_THREADS);
            int threads = clientIoThreads > 0 ?
                clientIoThreads : // 用户配置
                Math.max(4, SystemInfo.getCpuCores() + 1); // 默认cpu+1,至少4个
            NamedThreadFactory threadName = new NamedThreadFactory("CLI-IO", true);
            boolean useEpoll = getBooleanValue(TRANSPORT_USE_EPOLL);
            clientIOEventLoopGroup = useEpoll ? new EpollEventLoopGroup(threads, threadName)
                : new NioEventLoopGroup(threads, threadName);
            refCounter.putIfAbsent(clientIOEventLoopGroup, new AtomicInteger(0));
            // SelectStrategyFactory 未设置
        }
        refCounter.get(clientIOEventLoopGroup).incrementAndGet();
        return clientIOEventLoopGroup;
    }

    /**
     * 关闭客户端IO线程池
     */
    public synchronized static void closeClientIOEventGroup() {
        if (clientIOEventLoopGroup != null) {
            AtomicInteger ref = refCounter.get(clientIOEventLoopGroup);
            if (ref.decrementAndGet() <= 0) {
                if (!clientIOEventLoopGroup.isShutdown() && !clientIOEventLoopGroup.isShuttingDown()) {
                    clientIOEventLoopGroup.shutdownGracefully();
                }
                refCounter.remove(clientIOEventLoopGroup);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Closing Client EventLoopGroup, ref : 0");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Client EventLoopGroup still has ref : {}", ref.get());
                }
            }
        }
        clientIOEventLoopGroup = null;
    }

    /**
     * 得到服务端业务线程池
     *
     * @param config   服务端配置
     * @param executor 业务线程池
     * @return 服务端业务线程池
     */
    public static EventLoopGroup getServerBizEventLoopGroup(ServerTransportConfig config, Executor executor) {
        int bizThreads = config.getBizMaxThreads();
        return config.isUseEpoll() ?
            new EpollEventLoopGroup(config.getBizMaxThreads(), executor) :
            new NioEventLoopGroup(bizThreads, executor);
    }

    private static AdaptiveRecvByteBufAllocator recvByteBufAllocator = AdaptiveRecvByteBufAllocator.DEFAULT;

    private static ByteBufAllocator             byteBufAllocator     = ByteBufAllocator.DEFAULT;

    public static ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    public static ByteBuf getBuffer() {
        return byteBufAllocator.buffer();
    }

    public static ByteBuf getBuffer(int size) {
        return byteBufAllocator.buffer(size);
    }

    public static AdaptiveRecvByteBufAllocator getRecvByteBufAllocator() {
        return recvByteBufAllocator;
    }

    private static final AttributeKey<AbstractChannel> CHANNEL_KEY = AttributeKey.newInstance("sofa-channel");

    public static AbstractChannel getChannel(ChannelHandlerContext ctx) {
        Attribute<AbstractChannel> attr = ctx.channel().attr(CHANNEL_KEY);
        AbstractChannel sofaChannel = attr.get();
        if (sofaChannel == null) {
            sofaChannel = new NettyChannel(ctx);
            AbstractChannel old = attr.setIfAbsent(sofaChannel);
            if (old != null) {
                sofaChannel = old;
            }
        }
        return sofaChannel;
    }

    public static String toString(ByteBuf byteBuf) {
        if (byteBuf == null) {
            return null;
        }
        byte[] bs;
        int readIndex = byteBuf.readerIndex();
        if (byteBuf.hasArray()) {
            bs = byteBuf.array();
        } else {
            bs = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bs);
        }
        // 恢复Index
        byteBuf.readerIndex(readIndex);
        return new String(bs, RpcConstants.DEFAULT_CHARSET);
    }
}
