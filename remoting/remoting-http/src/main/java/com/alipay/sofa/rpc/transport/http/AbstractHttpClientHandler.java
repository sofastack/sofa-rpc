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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.codec.common.StringSerializer;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.AsyncRuntime;
import com.alipay.sofa.rpc.context.BaggageResolver;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ClientHandler;
import com.alipay.sofa.rpc.transport.netty.NettyByteBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * HTTP调用的客户端处理器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public abstract class AbstractHttpClientHandler implements ClientHandler {
    /**
     * 服务消费者配置
     */
    protected final ConsumerConfig consumerConfig;
    /**
     * 服务提供者信息
     */
    protected final ProviderInfo   providerInfo;
    /**
     * 请求
     */
    protected final SofaRequest    request;
    /**
     * 请求运行时的ClassLoader
     */
    protected ClassLoader          classLoader;
    /**
     * 线程上下文
     */
    protected RpcInternalContext   context;

    protected AbstractHttpClientHandler(ConsumerConfig consumerConfig, ProviderInfo providerInfo, SofaRequest request,
                                        RpcInternalContext context, ClassLoader classLoader) {
        this.consumerConfig = consumerConfig;
        this.providerInfo = providerInfo;
        this.request = request;
        this.context = context;
        this.classLoader = classLoader;
    }

    protected void recordClientElapseTime() {
        if (context != null) {
            Long startTime = (Long) context.removeAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME);
            if (startTime != null) {
                context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, RpcRuntimeContext.now() - startTime);
            }
        }
    }

    protected void pickupBaggage(SofaResponse response) {
        if (RpcInvokeContext.isBaggageEnable()) {
            RpcInvokeContext invokeCtx = null;
            if (context != null) {
                invokeCtx = (RpcInvokeContext) context.getAttachment(RpcConstants.HIDDEN_KEY_INVOKE_CONTEXT);
            }
            if (invokeCtx == null) {
                invokeCtx = RpcInvokeContext.getContext();
            } else {
                RpcInvokeContext.setContext(invokeCtx);
            }
            BaggageResolver.pickupFromResponse(invokeCtx, response);
        }
    }

    protected void decode(SofaResponse response) {
        AbstractByteBuf byteBuffer = response.getData();
        if (byteBuffer != null) {
            try {
                Map<String, String> context = new HashMap<String, String>(4);
                if (response.isError()) {
                    context.put(RemotingConstants.HEAD_RESPONSE_ERROR, response.isError() + "");
                    String errorMsg = StringSerializer.decode(byteBuffer.array());
                    response.setAppResponse(new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, errorMsg));
                } else {
                    context.put(RemotingConstants.HEAD_TARGET_SERVICE, request.getTargetServiceUniqueName());
                    context.put(RemotingConstants.HEAD_METHOD_NAME, request.getMethodName());
                    Serializer serializer = SerializerFactory.getSerializer(response.getSerializeType());
                    serializer.decode(byteBuffer, response, context);
                }
            } finally {
                byteBuffer.release();
                response.setData(null);
            }
        }
    }

    @Override
    public Executor getExecutor() {
        return AsyncRuntime.getAsyncThreadPool();
    }

    public void receiveHttpResponse(FullHttpResponse msg) {
        HttpHeaders headers = msg.headers();
        ByteBuf content = msg.content();
        NettyByteBuffer data = new NettyByteBuffer(content);
        try {
            if (msg.status() == HttpResponseStatus.OK) {
                // 正常返回
                final SofaResponse response = new SofaResponse();
                String isError = headers.get(RemotingConstants.HEAD_RESPONSE_ERROR);
                if (CommonUtils.isTrue(isError)) {
                    // 业务异常
                    String errorMsg = StringSerializer.decode(data.array());
                    Throwable throwable = new SofaRpcException(RpcErrorType.SERVER_BIZ, errorMsg);
                    response.setAppResponse(throwable);
                } else {
                    // 获取序列化类型
                    if (data.readableBytes() > 0) {
                        byte serializeType;
                        String codeName = headers.get(RemotingConstants.HEAD_SERIALIZE_TYPE);
                        if (codeName != null) {
                            serializeType = HttpTransportUtils.getSerializeTypeByName(codeName);
                        } else {
                            // HEAD_SERIALIZE_TYPE 没设置的话 再取 content-type 兜底下
                            String contentType = StringUtils.toString(headers.get(HttpHeaderNames.CONTENT_TYPE));
                            serializeType = HttpTransportUtils.getSerializeTypeByContentType(contentType);
                        }
                        response.setSerializeType(serializeType);
                        content.retain();
                        response.setData(data);
                    }
                }
                onResponse(response);
            } else {
                // 系统异常
                String errorMsg = StringSerializer.decode(data.array());
                Throwable throwable = new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, errorMsg);
                onException(throwable);
            }
        } catch (final Exception e) {
            onException(e);
        }
    }

    /**
     * On response received.
     *
     * @param result
     */
    public abstract void doOnResponse(final Object result);

    /**
     * On exception caught.
     *
     * @param e
     */
    public abstract void doOnException(final Throwable e);

    @Override
    public void onResponse(final Object response) {
        Executor executor = getExecutor();
        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    doOnResponse(response);
                }
            });
        } else {
            doOnResponse(response);
        }
    }

    @Override
    public void onException(final Throwable e) {
        Executor executor = getExecutor();
        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    doOnException(e);
                }
            });
        } else {
            doOnException(e);
        }
    }
}
