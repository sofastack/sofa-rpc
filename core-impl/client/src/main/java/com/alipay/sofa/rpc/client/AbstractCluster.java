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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.AsyncRuntime;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderInfoAddEvent;
import com.alipay.sofa.rpc.event.ProviderInfoRemoveEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateAllEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateEvent;
import com.alipay.sofa.rpc.filter.ConsumerInvoker;
import com.alipay.sofa.rpc.filter.FilterChain;
import com.alipay.sofa.rpc.listener.ConsumerStateListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.ClientTransport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_INVOKE_TIMEOUT;

/**
 * Abstract cluster, contains router chain, filter chain, address holder, connection holder and load balancer.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public abstract class AbstractCluster extends Cluster {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractCluster.class);

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public AbstractCluster(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    /**
     * 是否已启动(已建立连接)
     */
    protected volatile boolean initialized   = false;

    /**
     * 是否已经销毁（已经销毁不能再继续使用）
     */
    protected volatile boolean destroyed     = false;

    /**
     * 当前Client正在发送的调用数量
     */
    protected AtomicInteger    countOfInvoke = new AtomicInteger(0);

    /**
     * 路由列表
     */
    protected RouterChain      routerChain;
    /**
     * 负载均衡接口
     */
    protected LoadBalancer     loadBalancer;
    /**
     * 地址保持器
     */
    protected AddressHolder    addressHolder;
    /**
     * 连接管理器
     */
    protected ConnectionHolder connectionHolder;
    /**
     * 过滤器链
     */
    protected FilterChain      filterChain;

    @Override
    public synchronized void init() {
        if (initialized) { // 已初始化
            return;
        }
        // 构造Router链
        routerChain = RouterChain.buildConsumerChain(consumerBootstrap);
        // 负载均衡策略 考虑是否可动态替换？
        loadBalancer = LoadBalancerFactory.getLoadBalancer(consumerBootstrap);
        // 地址管理器
        addressHolder = AddressHolderFactory.getAddressHolder(consumerBootstrap);
        // 连接管理器
        connectionHolder = ConnectionHolderFactory.getConnectionHolder(consumerBootstrap);
        // 构造Filter链,最底层是调用过滤器
        this.filterChain = FilterChain.buildConsumerChain(this.consumerConfig,
            new ConsumerInvoker(consumerBootstrap));

        if (consumerConfig.isLazy()) { // 延迟连接
            if (LOGGER.isInfoEnabled(consumerConfig.getAppName())) {
                LOGGER.infoWithApp(consumerConfig.getAppName(), "Connection will be initialized when first invoke.");
            }
        }

        // 启动重连线程
        connectionHolder.init();
        try {
            // 得到服务端列表
            List<ProviderGroup> all = consumerBootstrap.subscribe();
            if (CommonUtils.isNotEmpty(all)) {
                // 初始化服务端连接（建立长连接)
                updateAllProviders(all);
            }
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException("Init provider's transport error!", e);
        }

        // 启动成功
        initialized = true;

        // 如果check=true表示强依赖
        if (consumerConfig.isCheck() && !isAvailable()) {
            throw new SofaRpcRuntimeException("The consumer is depend on alive provider " +
                "and there is no alive provider, you can ignore it " +
                "by ConsumerConfig.setCheck(boolean) (default is false)");
        }
    }

    /**
     * 检测状态
     */
    protected void checkClusterState() {
        if (destroyed) { // 已销毁
            throw new SofaRpcRuntimeException("Client has been destroyed!");
        }
        if (!initialized) { // 未初始化
            init();
        }
    }

    @Override
    public void addProvider(ProviderGroup providerGroup) {
        // 包装了各个组件的操作
        addressHolder.addProvider(providerGroup);
        connectionHolder.addProvider(providerGroup);
        if (EventBus.isEnable(ProviderInfoAddEvent.class)) {
            ProviderInfoAddEvent event = new ProviderInfoAddEvent(consumerConfig, providerGroup);
            EventBus.post(event);
        }
    }

    @Override
    public void removeProvider(ProviderGroup providerGroup) {
        // 包装了各个组件的操作
        addressHolder.removeProvider(providerGroup);
        connectionHolder.removeProvider(providerGroup);
        if (EventBus.isEnable(ProviderInfoRemoveEvent.class)) {
            ProviderInfoRemoveEvent event = new ProviderInfoRemoveEvent(consumerConfig, providerGroup);
            EventBus.post(event);
        }
    }

    @Override
    public void updateProviders(ProviderGroup providerGroup) {
        checkProviderInfo(providerGroup);
        ProviderGroup oldProviderGroup = addressHolder.getProviderGroup(providerGroup.getName());
        if (ProviderHelper.isEmpty(providerGroup)) {
            addressHolder.updateProviders(providerGroup);
            if (!ProviderHelper.isEmpty(oldProviderGroup)) {
                if (LOGGER.isWarnEnabled(consumerConfig.getAppName())) {
                    LOGGER.warnWithApp(consumerConfig.getAppName(), "Provider list is emptied, may be all " +
                        "providers has been closed, or this consumer has been add to blacklist");
                    closeTransports();
                }
            }
        } else {
            addressHolder.updateProviders(providerGroup);
            connectionHolder.updateProviders(providerGroup);
        }
        if (EventBus.isEnable(ProviderInfoUpdateEvent.class)) {
            ProviderInfoUpdateEvent event = new ProviderInfoUpdateEvent(consumerConfig, oldProviderGroup, providerGroup);
            EventBus.post(event);
        }
    }

    @Override
    public void updateAllProviders(List<ProviderGroup> providerGroups) {
        List<ProviderGroup> oldProviderGroups = new ArrayList<ProviderGroup>(addressHolder.getProviderGroups());
        int count = 0;
        if (providerGroups != null) {
            for (ProviderGroup providerGroup : providerGroups) {
                checkProviderInfo(providerGroup);
                count += providerGroup.size();
            }
        }
        if (count == 0) {
            Collection<ProviderInfo> currentProviderList = currentProviderList();
            addressHolder.updateAllProviders(providerGroups);
            if (CommonUtils.isNotEmpty(currentProviderList)) {
                if (LOGGER.isWarnEnabled(consumerConfig.getAppName())) {
                    LOGGER.warnWithApp(consumerConfig.getAppName(), "Provider list is emptied, may be all " +
                        "providers has been closed, or this consumer has been add to blacklist");
                    closeTransports();
                }
            }
        } else {
            addressHolder.updateAllProviders(providerGroups);
            connectionHolder.updateAllProviders(providerGroups);
        }
        if (EventBus.isEnable(ProviderInfoUpdateAllEvent.class)) {
            ProviderInfoUpdateAllEvent event = new ProviderInfoUpdateAllEvent(consumerConfig, oldProviderGroups,
                providerGroups);
            EventBus.post(event);
        }
    }

    /**
     * 检测服务节点的一些信息
     *
     * @param providerGroup 服务列表分组
     */
    protected void checkProviderInfo(ProviderGroup providerGroup) {
        List<ProviderInfo> providerInfos = providerGroup == null ? null : providerGroup.getProviderInfos();
        if (CommonUtils.isEmpty(providerInfos)) {
            return;
        }
        Iterator<ProviderInfo> iterator = providerInfos.iterator();
        while (iterator.hasNext()) {
            ProviderInfo providerInfo = iterator.next();
            if (!StringUtils.equals(providerInfo.getProtocolType(), consumerConfig.getProtocol())) {
                if (LOGGER.isWarnEnabled(consumerConfig.getAppName())) {
                    LOGGER.warnWithApp(consumerConfig.getAppName(),
                        "Unmatched protocol between consumer [{}] and provider [{}].",
                        consumerConfig.getProtocol(), providerInfo.getProtocolType());
                }
            }
        }
    }

    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        SofaResponse response = null;
        try {
            // 做一些初始化检查，例如未连接可以连接
            checkClusterState();
            // 开始调用
            countOfInvoke.incrementAndGet(); // 计数+1         
            response = doInvoke(request);
            return response;
        } catch (SofaRpcException e) {
            // 客户端收到异常（客户端自己的异常）
            throw e;
        } finally {
            countOfInvoke.decrementAndGet(); // 计数-1
        }
    }

    /**
     * 子类实现各自逻辑的调用，例如重试等
     *
     * @param msg Request对象
     * @return 调用结果
     * @throws SofaRpcException rpc异常
     */
    protected abstract SofaResponse doInvoke(SofaRequest msg) throws SofaRpcException;

    /**
     * 检查服务端版本，特殊处理
     *
     * @param providerInfo 服务端
     * @param request      请求对象
     */
    protected void checkProviderVersion(ProviderInfo providerInfo, SofaRequest request) {

    }

    /**
     * 上一次连接，目前是记录整个接口的，是否需要方法级的？？
     */
    private volatile ProviderInfo lastProviderInfo;

    /**
     * 根据规则进行负载均衡
     *
     * @param message 调用对象
     * @return 一个可用的provider
     * @throws SofaRpcException rpc异常
     */
    protected ProviderInfo select(SofaRequest message) throws SofaRpcException {
        return select(message, null);
    }

    /**
     * 根据规则进行负载均衡
     *
     * @param message              调用对象
     * @param invokedProviderInfos 已调用列表
     * @return 一个可用的provider
     * @throws SofaRpcException rpc异常
     */
    protected ProviderInfo select(SofaRequest message, List<ProviderInfo> invokedProviderInfos)
        throws SofaRpcException {
        // 粘滞连接，当前连接可用
        if (consumerConfig.isSticky()) {
            if (lastProviderInfo != null) {
                ProviderInfo providerInfo = lastProviderInfo;
                ClientTransport lastTransport = connectionHolder.getAvailableClientTransport(providerInfo);
                if (lastTransport != null && lastTransport.isAvailable()) {
                    checkAlias(providerInfo, message);
                    return providerInfo;
                }
            }
        }
        // 原始服务列表数据 --> 路由结果
        List<ProviderInfo> providerInfos = routerChain.route(message, null);

        //保存一下原始地址,为了打印
        List<ProviderInfo> orginalProviderInfos = new ArrayList<ProviderInfo>(providerInfos);

        if (CommonUtils.isEmpty(providerInfos)) {
            throw noAvailableProviderException(message.getTargetServiceUniqueName());
        }
        if (CommonUtils.isNotEmpty(invokedProviderInfos) && providerInfos.size() > invokedProviderInfos.size()) { // 总数大于已调用数
            providerInfos.removeAll(invokedProviderInfos);// 已经调用异常的本次不再重试
        }

        String targetIP = null;
        ProviderInfo providerInfo;
        RpcInternalContext context = RpcInternalContext.peekContext();
        if (context != null) {
            targetIP = (String) RpcInternalContext.getContext().getAttachment(RpcConstants.HIDDEN_KEY_PINPOINT);
        }
        if (StringUtils.isNotBlank(targetIP)) {
            // 如果指定了调用地址
            providerInfo = selectPinpointProvider(targetIP, providerInfos);
            if (providerInfo == null) {
                // 指定的不存在
                throw unavailableProviderException(message.getTargetServiceUniqueName(), targetIP);
            }
            ClientTransport clientTransport = selectByProvider(message, providerInfo);
            if (clientTransport == null) {
                // 指定的不存在或已死，抛出异常
                throw unavailableProviderException(message.getTargetServiceUniqueName(), targetIP);
            }
            return providerInfo;
        } else {
            do {
                // 再进行负载均衡筛选
                providerInfo = loadBalancer.select(message, providerInfos);
                ClientTransport transport = selectByProvider(message, providerInfo);
                if (transport != null) {
                    return providerInfo;
                }
                providerInfos.remove(providerInfo);
            } while (!providerInfos.isEmpty());
        }
        throw unavailableProviderException(message.getTargetServiceUniqueName(),
            convertProviders2Urls(orginalProviderInfos));
    }

    /**
     * Select provider.
     *
     * @param targetIP the target ip
     * @return the provider
     */
    protected ProviderInfo selectPinpointProvider(String targetIP, List<ProviderInfo> providerInfos) {
        ProviderInfo tp = ProviderHelper.toProviderInfo(targetIP);
        for (ProviderInfo providerInfo : providerInfos) {
            if (providerInfo.getHost().equals(tp.getHost())
                && StringUtils.equals(providerInfo.getProtocolType(), tp.getProtocolType())
                && providerInfo.getPort() == tp.getPort()) {
                return providerInfo;
            }
        }
        return null;
    }

    /**
     * 找不到可用的服务列表的异常
     *
     * @param serviceKey 服务关键字
     * @return 服务端
     */
    protected SofaRouteException noAvailableProviderException(String serviceKey) {
        return new SofaRouteException(LogCodes.getLog(LogCodes.ERROR_NO_AVAILBLE_PROVIDER, serviceKey));
    }

    /**
     * 指定地址不可用
     *
     * @param serviceKey 服务关键字
     * @return 服务端
     */
    protected SofaRouteException unavailableProviderException(String serviceKey, String providerInfo) {
        return new SofaRouteException(LogCodes.getLog(LogCodes.ERROR_TARGET_URL_INVALID, serviceKey, providerInfo));
    }

    /**
     * 得到provider得到连接
     *
     * @param message      调用对象
     * @param providerInfo 指定Provider
     * @return 一个可用的transport或者null
     */
    protected ClientTransport selectByProvider(SofaRequest message, ProviderInfo providerInfo) {
        ClientTransport transport = connectionHolder.getAvailableClientTransport(providerInfo);
        if (transport != null) {
            if (transport.isAvailable()) {
                lastProviderInfo = providerInfo;
                checkAlias(providerInfo, message); //检查分组
                return transport;
            } else {
                connectionHolder.setUnavailable(providerInfo, transport);
            }
        }
        return null;
    }

    /**
     * 检查分组映射
     *
     * @param providerInfo 服务端
     * @param message      请求对象
     */
    protected void checkAlias(ProviderInfo providerInfo, SofaRequest message) {

    }

    /**
     * 发起调用链
     *
     * @param providerInfo 服务端信息
     * @param request      请求对象
     * @return 执行后返回的响应
     * @throws SofaRpcException 请求RPC异常
     */
    protected SofaResponse filterChain(ProviderInfo providerInfo, SofaRequest request) throws SofaRpcException {
        RpcInternalContext context = RpcInternalContext.getContext();
        context.setProviderInfo(providerInfo);
        return filterChain.invoke(request);
    }

    @Override
    public SofaResponse sendMsg(ProviderInfo providerInfo, SofaRequest request) throws SofaRpcException {
        ClientTransport clientTransport = connectionHolder.getAvailableClientTransport(providerInfo);
        if (clientTransport != null && clientTransport.isAvailable()) {
            return doSendMsg(providerInfo, clientTransport, request);
        } else {
            throw unavailableProviderException(request.getTargetServiceUniqueName(), providerInfo.getOriginUrl());
        }
    }

    /**
     * 调用客户端
     *
     * @param transport 客户端连接
     * @param request   Request对象
     * @return 调用结果
     * @throws SofaRpcException rpc异常
     */
    protected SofaResponse doSendMsg(ProviderInfo providerInfo, ClientTransport transport,
                                     SofaRequest request) throws SofaRpcException {
        RpcInternalContext context = RpcInternalContext.getContext();
        // 添加调用的服务端远程地址
        RpcInternalContext.getContext().setRemoteAddress(providerInfo.getHost(), providerInfo.getPort());
        try {
            checkProviderVersion(providerInfo, request); // 根据服务端版本特殊处理
            String invokeType = request.getInvokeType();
            int timeout = resolveTimeout(request, consumerConfig, providerInfo);

            SofaResponse response = null;
            // 同步调用
            if (RpcConstants.INVOKER_TYPE_SYNC.equals(invokeType)) {
                long start = RpcRuntimeContext.now();
                try {
                    response = transport.syncSend(request, timeout);
                } finally {
                    if (RpcInternalContext.isAttachmentEnable()) {
                        long elapsed = RpcRuntimeContext.now() - start;
                        context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, elapsed);
                    }
                }
            }
            // 单向调用
            else if (RpcConstants.INVOKER_TYPE_ONEWAY.equals(invokeType)) {
                long start = RpcRuntimeContext.now();
                try {
                    transport.oneWaySend(request, timeout);
                    response = buildEmptyResponse(request);
                } finally {
                    if (RpcInternalContext.isAttachmentEnable()) {
                        long elapsed = RpcRuntimeContext.now() - start;
                        context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, elapsed);
                    }
                }
            }
            // Callback调用
            else if (RpcConstants.INVOKER_TYPE_CALLBACK.equals(invokeType)) {
                // 调用级别回调监听器
                SofaResponseCallback sofaResponseCallback = request.getSofaResponseCallback();
                if (sofaResponseCallback == null) {
                    SofaResponseCallback methodResponseCallback = consumerConfig
                        .getMethodOnreturn(request.getMethodName());
                    if (methodResponseCallback != null) { // 方法的Callback
                        request.setSofaResponseCallback(methodResponseCallback);
                    }
                }
                // 记录发送开始时间
                context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME, RpcRuntimeContext.now());
                // 开始调用
                transport.asyncSend(request, timeout);
                response = buildEmptyResponse(request);
            }
            // Future调用
            else if (RpcConstants.INVOKER_TYPE_FUTURE.equals(invokeType)) {
                // 记录发送开始时间
                context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME, RpcRuntimeContext.now());
                // 开始调用
                ResponseFuture future = transport.asyncSend(request, timeout);
                // 放入线程上下文
                RpcInternalContext.getContext().setFuture(future);
                response = buildEmptyResponse(request);
            } else {
                throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Unknown invoke type:" + invokeType);
            }
            return response;
        } catch (SofaRpcException e) {
            throw e;
        } catch (Throwable e) { // 客户端其它异常
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e);
        }
    }

    private SofaResponse buildEmptyResponse(SofaRequest request) {
        SofaResponse response = new SofaResponse();
        Method method = request.getMethod();
        if (method != null) {
            response.setAppResponse(ClassUtils.getDefaultPrimitiveValue(method.getReturnType()));
        }
        return response;
    }

    /**
     * 决定超时时间
     *
     * @param request        请求
     * @param consumerConfig 客户端配置
     * @param providerInfo   服务提供者信息
     * @return 调用超时
     */
    private int resolveTimeout(SofaRequest request, ConsumerConfig consumerConfig, ProviderInfo providerInfo) {
        // 先去调用级别配置
        Integer timeout = request.getTimeout();
        if (timeout == null) {
            // 取客户端配置（先方法级别再接口级别）
            timeout = consumerConfig.getMethodTimeout(request.getMethodName());
            if (timeout == null || timeout < 0) {
                // 再取服务端配置
                timeout = (Integer) providerInfo.getDynamicAttr(ATTR_TIMEOUT);
                if (timeout == null) {
                    // 取框架默认值
                    timeout = getIntValue(CONSUMER_INVOKE_TIMEOUT);
                }
            }
        }
        return timeout;
    }

    @Override
    public void destroy() {
        destroy(null);
    }

    @Override
    public void destroy(DestroyHook hook) {
        if (destroyed) {
            return;
        }
        if (hook != null) {
            hook.postDestroy();
        }
        if (connectionHolder != null) {
            connectionHolder.destroy(new GracefulDestroyHook());
        }
        destroyed = true;
        initialized = false;
        if (hook != null) {
            hook.postDestroy();
        }
    }

    /**
     * 关闭连接<br>
     * 注意：关闭有风险，可能有正在调用的请求，建议判断下isAvailable()
     */
    protected void closeTransports() {
        if (connectionHolder != null) {
            connectionHolder.closeAllClientTransports(new GracefulDestroyHook());
        }
    }

    /**
     * 优雅关闭的钩子
     */
    protected class GracefulDestroyHook implements DestroyHook {
        @Override
        public void preDestroy() {
            // 准备关闭连接
            int count = countOfInvoke.get();
            final int timeout = consumerConfig.getDisconnectTimeout(); // 等待结果超时时间
            if (count > 0) { // 有正在调用的请求
                long start = RpcRuntimeContext.now();
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("There are {} outstanding call in client, will close transports util return",
                        count);
                }
                while (countOfInvoke.get() > 0 && RpcRuntimeContext.now() - start < timeout) { // 等待返回结果
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }

        @Override
        public void postDestroy() {
        }
    }

    @Override
    public boolean isAvailable() {
        if (destroyed || !initialized) {
            return false;
        }
        List<ProviderGroup> providerGroups = addressHolder.getProviderGroups();
        if (CommonUtils.isEmpty(providerGroups)) {
            return false;
        }
        for (ProviderGroup entry : providerGroups) {
            List<ProviderInfo> providerInfos = entry.getProviderInfos();
            for (ProviderInfo providerInfo : providerInfos) {
                ClientTransport transport = connectionHolder.getAvailableClientTransport(providerInfo);
                if (transport != null && transport.isAvailable()) {
                    return true; // 只有有1个可用 即可返回
                } else {
                    connectionHolder.setUnavailable(providerInfo, transport);
                }
            }
        }
        return false;
    }

    @Override
    public void checkStateChange(boolean originalState) {
        if (originalState) { // 原来可以
            if (!isAvailable()) { // 变不可以
                notifyStateChangeToUnavailable();
            }
        } else { // 原来不可用
            if (isAvailable()) { // 变成可用
                notifyStateChangeToAvailable();
            }
        }
    }

    /**
     * 通知状态变成不可用,主要是：<br>
     * 1.注册中心删除，更新节点后变成不可用时<br>
     * 2.连接断线后（心跳+调用），如果是可用节点为空
     */
    public void notifyStateChangeToUnavailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
            AsyncRuntime.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onprepear) {
                        try {
                            listener.onUnavailable(consumerBootstrap.getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to unavailable");
                        }
                    }
                }
            });
        }
    }

    /**
     * 通知状态变成可用,主要是：<br>
     * 1.启动成功变成可用时<br>
     * 2.注册中心增加，更新节点后变成可用时<br>
     * 3.重连上从一个可用节点都没有-->有可用节点时
     */
    public void notifyStateChangeToAvailable() {
        final List<ConsumerStateListener> onprepear = consumerConfig.getOnAvailable();
        if (onprepear != null) {
            AsyncRuntime.getAsyncThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // 状态变化通知监听器
                    for (ConsumerStateListener listener : onprepear) {
                        try {
                            listener.onAvailable(consumerBootstrap.getProxyIns());
                        } catch (Exception e) {
                            LOGGER.error("Failed to notify consumer state listener when state change to available");
                        }
                    }
                }
            });
        }
    }

    /**
     * 获取当前的Provider全部列表（包括连上和没连上的），比较费性能，慎用
     *
     * @return 当前的Provider列表
     */
    public Collection<ProviderInfo> currentProviderList() {
        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();
        List<ProviderGroup> providerGroups = addressHolder.getProviderGroups();
        if (CommonUtils.isNotEmpty(providerGroups)) {
            for (ProviderGroup entry : providerGroups) {
                providerInfos.addAll(entry.getProviderInfos());
            }
        }
        return providerInfos;
    }

    private String convertProviders2Urls(List<ProviderInfo> providerInfos) {

        StringBuilder sb = new StringBuilder();
        if (CommonUtils.isNotEmpty(providerInfos)) {
            for (ProviderInfo providerInfo : providerInfos) {
                sb.append(providerInfo).append(",");
            }
        }

        return sb.toString();
    }

    /**
     * @return the consumerConfig
     */
    public ConsumerConfig<?> getConsumerConfig() {
        return consumerConfig;
    }

    @Override
    public AddressHolder getAddressHolder() {
        return addressHolder;
    }

    @Override
    public ConnectionHolder getConnectionHolder() {
        return connectionHolder;
    }

    @Override
    public FilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public RouterChain getRouterChain() {
        return routerChain;
    }
}
