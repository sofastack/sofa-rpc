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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitClient;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitClientConfig;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitClientFactory;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitHandler;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitLauncher;
import com.alipay.sofa.rpc.transmit.ip.IpTransmitResult;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预热转发
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "ipTransmit", order = -7000)
@AutoActive(providerSide = true)
public class IpTransmitFilter extends Filter {

    /**
     * Logger for IpTransmitFilter
     **/
    private static final Logger                LOGGER          = LoggerFactory.getLogger(IpTransmitFilter.class);

    /**
     * 协议：端口
     */
    private ConcurrentHashMap<String, Integer> protocolPortMap = new ConcurrentHashMap<String, Integer>();

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        if (IpTransmitLauncher.isLaunched()) {
            ProviderConfig config = (ProviderConfig) invoker.getConfig();
            String appName = config.getAppName();
            IpTransmitHandler handler = IpTransmitLauncher.getHandler(appName);
            if (handler != null && handler.isStarted()) {
                IpTransmitResult result = handler.judgeRequestTransmitResult();
                if (result != null && result.isTransmit()) {
                    // 需要预热转发
                    return doTransmitInvoke(invoker, request, config, result);
                }
            }
            // 未加载预热转发，或者转发已结束，或者不需要转发
            return invoker.invoke(request);
        } else {
            // 未开启预热转发
            return invoker.invoke(request);
        }
    }

    /**
     * 执行转发
     *
     * @param originRequest  原始请求
     * @param providerConfig 原始服务提供者配置
     * @param result         转发结果地址
     * @return 转发结果
     * @throws SofaRpcException 转发异常
     */
    protected SofaResponse doTransmitInvoke(FilterInvoker invoker, SofaRequest originRequest,
                                            ProviderConfig providerConfig, IpTransmitResult result)
        throws SofaRpcException {

        String protocol = (String) originRequest.getRequestProp(RemotingConstants.HEAD_PROTOCOL);
        // 指定转发地址
        Integer port = protocolPortMap.get(protocol);
        if (port == null) {
            List<ServerConfig> serverConfigs = providerConfig.getServer();
            for (ServerConfig serverConfig : serverConfigs) {
                if (protocol.equals(serverConfig.getProtocol())) {
                    port = serverConfig.getPort();
                    protocolPortMap.put(protocol, port);
                    break;
                }
            }
        }
        if (port == null) {
            String appName = providerConfig.getAppName();
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.ERROR_TRANSMIT_PARSE_URL));
            }
            // 不转发
            return invoker.invoke(originRequest);
        } else {
            String transmitUrl = result.getTransmitAddress() + ":" + port;
            RpcInternalContext.getContext().setAttachment(RpcConstants.HIDDEN_KEY_PINPOINT, transmitUrl);
            // 构造转发客户端和转发请求
            IpTransmitClient client = getTransmitClient(providerConfig, protocol);
            SofaRequest transmitRequest = convertToTransmitRequest(originRequest, providerConfig, result);
            // 转发调用
            return client.invoke(transmitRequest);
        }
    }

    /**
     * 复制一份转发请求
     *
     * @param originRequest  原始请求
     * @param providerConfig 当前服务提供者配置
     * @return 转发请求
     */
    private SofaRequest convertToTransmitRequest(SofaRequest originRequest, ProviderConfig providerConfig,
                                                 IpTransmitResult result) {
        SofaRequest transmitRequest = new SofaRequest();
        transmitRequest.setTargetAppName(originRequest.getTargetAppName());
        transmitRequest.setMethodArgs(originRequest.getMethodArgs());
        transmitRequest.setMethodArgSigs(originRequest.getMethodArgSigs());
        transmitRequest.setMethodName(originRequest.getMethodName());
        transmitRequest.setTargetServiceUniqueName(originRequest.getTargetServiceUniqueName());
        transmitRequest.setInterfaceName(providerConfig.getInterfaceId());
        transmitRequest.setSerializeType(originRequest.getSerializeType());
        transmitRequest.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        // 超时设置为独立的转发超时
        transmitRequest.setTimeout(result.getTransmitTimeout());
        return transmitRequest;
    }

    /**
     * 获取转发客户端（未初始化）
     *
     * @param providerConfig 当前服务提供者配置
     * @param protocol       协议
     * @return 转发客户端
     */
    private IpTransmitClient getTransmitClient(ProviderConfig providerConfig, String protocol) {

        IpTransmitClientConfig clientConfig = new IpTransmitClientConfig();
        clientConfig.setProtocol(protocol);
        clientConfig.setInterfaceId(providerConfig.getInterfaceId());
        clientConfig.setAppName(providerConfig.getAppName());
        clientConfig.setUniqueId(providerConfig.getUniqueId());
        clientConfig.setPort(protocolPortMap.get(protocol));

        return IpTransmitClientFactory.getTransmitClient(clientConfig);
    }
}