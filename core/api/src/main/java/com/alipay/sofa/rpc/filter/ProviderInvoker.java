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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 服务端调用业务实现类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ProviderInvoker<T> extends FilterInvoker {

    /**
     * 日志
     */
    private static final Logger     LOGGER = LoggerFactory
                                               .getLogger(ProviderInvoker.class);

    /**
     * The Provider config.
     */
    private final ProviderConfig<T> providerConfig;

    private static Field            causeField;

    static {
        try {
            causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
        } catch (Exception e) {
            causeField = null;
            LOGGER.warnWithApp(null, "error  fetch causeField in ProviderInvoker", e);
        }
    }

    /**
     * Instantiates a new Provider invoke filter.
     *
     * @param providerConfig the provider config
     */
    public ProviderInvoker(ProviderConfig<T> providerConfig) {
        super(providerConfig);
        this.providerConfig = providerConfig;
    }

    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {

        /*// 将接口的<sofa:param />的配置复制到RpcInternalContext TODO
        RpcInternalContext context = RpcInternalContext.getContext();
        Map params = providerConfig.getParameters();
        if (params != null) {
           context.setAttachments(params);
        }
        // 将方法的<sofa:param />的配置复制到invocation
        String methodName = request.getMethodName();
        params = (Map) providerConfig.getMethodConfigValue(methodName, SofaConstants.CONFIG_KEY_PARAMS);
        if (params != null) {
           context.setAttachments(params);
        }*/

        SofaResponse sofaResponse = new SofaResponse();
        long startTime = RpcRuntimeContext.now();
        try {
            // 反射 真正调用业务代码
            Method method = request.getMethod();
            if (method == null) {
                throw new SofaRpcException(RpcErrorType.SERVER_FILTER,
                    LogCodes.getLog(LogCodes.ERROR_NEED_DECODE_METHOD));
            }
            Object result = method.invoke(providerConfig.getRef(), request.getMethodArgs());

            sofaResponse.setAppResponse(result);
        } catch (IllegalArgumentException e) { // 非法参数，可能是实现类和接口类不对应)
            sofaResponse.setErrorMsg(e.getMessage());
        } catch (IllegalAccessException e) { // 如果此 Method 对象强制执行 Java 语言访问控制，并且底层方法是不可访问的
            sofaResponse.setErrorMsg(e.getMessage());
            //        } catch (NoSuchMethodException e) { // 如果找不到匹配的方法
            //            sofaResponse.setErrorMsg(e.getMessage());
            //        } catch (ClassNotFoundException e) { // 如果指定的类加载器无法定位该类
            //            sofaResponse.setErrorMsg(e.getMessage());
        } catch (InvocationTargetException e) { // 业务代码抛出异常
            cutCause(e.getCause());
            sofaResponse.setAppResponse(e.getCause());
        } finally {
            if (RpcInternalContext.isAttachmentEnable()) {
                long endTime = RpcRuntimeContext.now();
                RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE,
                    endTime - startTime);
            }
        }

        return sofaResponse;
    }

    /**
     * 把业务层抛出的业务异常或者RuntimeException/Error，
     * 截断Cause，以免客户端因为无法找到cause类而出现反序列化失败.
     */
    public void cutCause(Throwable bizException) {
        if (causeField == null) {
            return;
        }

        Throwable rootCause = bizException;
        while (null != rootCause.getCause()) {
            rootCause = rootCause.getCause();
        }

        if (rootCause != bizException) {
            bizException.setStackTrace(rootCause.getStackTrace());
            try {
                causeField.set(bizException, bizException); // SELF-CAUSE
            } catch (Exception e) {
                LOGGER.warnWithApp(null, LogCodes.getLog(LogCodes.WARN_PROVIDER_CUT_CAUSE), e);
            }
        }
    }
}
