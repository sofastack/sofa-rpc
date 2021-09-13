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
package com.alipay.sofa.rpc.protocol;

import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alipay.sofa.rpc.protocol.telnet.TelnetCommandHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.ArrayList;
import java.util.List;

public class NettyTelnetServer {
    private int port;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;
    public static final List<String> CHANNEL_QUIT = new ArrayList<>();

    static {
        CHANNEL_QUIT.add("quit");
        CHANNEL_QUIT.add("q");
        CHANNEL_QUIT.add("exit");
    }

    public NettyTelnetServer(int port) {
        this.port = port;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
    }

    public void open() throws InterruptedException {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new NettyTelnetInitializer());
        channel = serverBootstrap.bind(port).sync().channel();
    }

    public void close() {
        channel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    static class NettyTelnetInitializer extends ChannelInitializer<SocketChannel> {
        private static StringDecoder DECODER = new StringDecoder();
        private static StringEncoder ENCODER = new StringEncoder();

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {

            ChannelPipeline pipeline = channel.pipeline();
            // Add the text line codec combination first
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            // 添加编码和解码的类
            pipeline.addLast(ENCODER);
            pipeline.addLast(DECODER);
            // 添加处理业务的类
            pipeline.addLast(new NettyTelnetHandler());
        }

    }

    static class NettyTelnetHandler extends SimpleChannelInboundHandler<String> {

        private static TelnetCommandHandler telnetCommandHandler = new TelnetCommandHandler();

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            ctx.write(telnetCommandHandler.promptMessage());
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            if (CHANNEL_QUIT.contains(msg)) {
                ctx.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            ctx.write(telnetCommandHandler.responseMessage(msg));
            ctx.flush();
        }
    }
}
