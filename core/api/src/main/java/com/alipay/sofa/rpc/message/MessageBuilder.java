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
package com.alipay.sofa.rpc.message;

import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.CodecUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;

import java.lang.reflect.Method;

/**
 * Build of message
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class MessageBuilder {

    /**
     * 构建请求，常用于代理类拦截
     *
     * @param clazz    接口类
     * @param method   方法名
     * @param argTypes 方法参数类型
     * @param args     方法参数值
     * @return 远程调用请求
     * @deprecated use {@link #buildSofaRequest(Class, Method, Class[], Object[])}
     */
    @Deprecated
    public static SofaRequest buildSofaRequest(Class<?> clazz, String method, Class[] argTypes, Object[] args) {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(clazz.getName());
        request.setMethodName(method);
        request.setMethodArgs(args == null ? CodecUtils.EMPTY_OBJECT_ARRAY : args);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
        return request;
    }

    /**
     * 构建请求，常用于代理类拦截
     *
     * @param clazz    接口类
     * @param method   方法
     * @param argTypes 方法参数类型
     * @param args     方法参数值
     * @return 远程调用请求
     */
    public static SofaRequest buildSofaRequest(Class<?> clazz, Method method, Class[] argTypes, Object[] args) {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(clazz.getName());
        request.setMethodName(method.getName());
        request.setMethod(method);
        request.setMethodArgs(args == null ? CodecUtils.EMPTY_OBJECT_ARRAY : args);
        request.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
        return request;
    }

    /**
     * 构建rpc错误结果
     *
     * @param errorMsg 错误消息
     * @return rpc结果
     */
    public static SofaResponse buildSofaErrorResponse(String errorMsg) {
        SofaResponse sofaResponse = new SofaResponse();
        sofaResponse.setErrorMsg(errorMsg);

        return sofaResponse;
    }
}
