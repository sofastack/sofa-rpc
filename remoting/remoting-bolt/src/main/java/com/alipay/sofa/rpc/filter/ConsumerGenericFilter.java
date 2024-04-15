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

import com.alipay.sofa.rpc.api.GenericContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * 客户端泛化调用处理filter
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "consumerGeneric", order = -18000)
@AutoActive(consumerSide = true)
public class ConsumerGenericFilter extends Filter {

    /**
     * 方法名 $invoke
     */
    private static final String METHOD_INVOKE         = "$invoke";
    /**
     * 方法名 $genericInvoke
     */
    private static final String METHOD_GENERIC_INVOKE = "$genericInvoke";

    private final static String REVISE_KEY            = "generic.revise";

    private final static String REVISE_VALUE          = "true";

    /**
     * 是否自动加载
     *
     * @param invoker 调用器
     * @return 是否加载本过滤器
     */
    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        ConsumerConfig consumerConfig = (ConsumerConfig) invoker.getConfig();
        return consumerConfig.isGeneric();
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        try {

            final String revised = (String) request.getRequestProp(REVISE_KEY);
            //if has revised, invoke directly
            if (REVISE_VALUE.equals(revised)) {
                return invoker.invoke(request);
            }
            String type = getSerializeFactoryType(request.getMethodName(), request.getMethodArgs());
            request.addRequestProp(RemotingConstants.HEAD_GENERIC_TYPE, type);

            // 修正超时时间
            Long clientTimeout = getClientTimeoutFromGenericContext(request.getMethodName(),
                request.getMethodArgs());
            if (clientTimeout != null && clientTimeout != 0) {
                request.setTimeout(clientTimeout.intValue());
            }

            // 修正请求对象
            Object[] genericArgs = request.getMethodArgs();
            String methodName = (String) genericArgs[0];
            String[] argTypes = (String[]) genericArgs[1];
            Object[] args = (Object[]) genericArgs[2];

            request.setMethodName(methodName);
            request.setMethodArgSigs(argTypes);
            request.setMethodArgs(args);

            // 修正类型
            ConsumerConfig consumerConfig = (ConsumerConfig) invoker.getConfig();
            String invokeType = consumerConfig.getMethodInvokeType(methodName);
            request.setInvokeType(invokeType);
            request.addRequestProp(RemotingConstants.HEAD_INVOKE_TYPE, invokeType);
            request.addRequestProp(REVISE_KEY, REVISE_VALUE);
            return invoker.invoke(request);
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_FILTER, e.getMessage(), e);
        }
    }

    private String getSerializeFactoryType(String method, Object[] args) throws SofaRpcException {
        if (METHOD_INVOKE.equals(method)) {
            // 方法名为 $invoke
            return RemotingConstants.SERIALIZE_FACTORY_NORMAL;
        } else if (METHOD_GENERIC_INVOKE.equals(method)) {
            if (args.length == 3) {
                // 方法名为$genericInvoke,且长度为3
                return RemotingConstants.SERIALIZE_FACTORY_GENERIC;
            } else if (args.length == 4) {
                // 方法名为$genericInvoke,且长度为4
                if (args[3] instanceof GenericContext) {
                    return RemotingConstants.SERIALIZE_FACTORY_GENERIC;
                }
                // 第四个参数是类型定义
                if (args[3] instanceof Class) {
                    return RemotingConstants.SERIALIZE_FACTORY_MIX;
                }
            } else if (args.length == 5) {
                // 方法名为$genericInvoke,且长度为5
                return RemotingConstants.SERIALIZE_FACTORY_MIX;
            }
        }
        throw new SofaRpcException(RpcErrorType.CLIENT_FILTER, "Unsupported method of generic service");
    }

    private Long getClientTimeoutFromGenericContext(String method, Object[] args) throws SofaRpcException {
        if (METHOD_GENERIC_INVOKE.equals(method)) {
            if (args.length == 4 && args[3] instanceof GenericContext) {
                return ((GenericContext) args[3]).getClientTimeout();
            } else if (args.length == 5 && args[4] instanceof GenericContext) {
                return ((GenericContext) args[4]).getClientTimeout();
            }
        }
        return null;
    }
}
