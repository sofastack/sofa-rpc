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
package com.alipay.sofa.rpc.api;

/**
 * 泛化调用的接口<br>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:caojie.cj@antfin.com">CaoJie</a>
 */
public interface GenericService {

    /**
     * 泛化调用
     *
     * @param methodName 调用方法名
     * @param argTypes   参数类型
     * @param args       方法参数，参数不能是GenericObject类型
     * @return 正常类型（不能是GenericObject类型）
     */
    Object $invoke(String methodName, String[] argTypes, Object[] args);

    /**
     * 支持参数类型无法在类加载器加载情况的泛化调用
     *
     * @param methodName 调用方法名
     * @param argTypes   参数类型
     * @param args       方法参数,参数类型支持GenericObject
     * @return 除了JDK等内置类型，其它对象是GenericObject类型
     */
    Object $genericInvoke(String methodName, String[] argTypes, Object[] args);

    /**
     * 支持参数类型无法在类加载器加载情况的泛化调用
     *
     * @param methodName 调用方法名
     * @param argTypes   参数类型
     * @param args       方法参数,参数类型支持GenericObject
     * @param clazz      返回类型
     * @return 返回指定的T类型返回对象
     */
    <T> T $genericInvoke(String methodName, String[] argTypes, Object[] args, Class<T> clazz);

    /**
     * 支持参数类型无法在类加载器加载情况的泛化调用
     *
     * @param methodName 调用方法名
     * @param argTypes   参数类型
     * @param args       方法参数,参数类型支持GenericObject
     * @param context    上下文，传递超时以及LDC相关信息
     * @return 除了JDK等内置类型，其它对象是GenericObject类型
     * @deprecated Use RpcInvokeContext instead of GenericContext
     */
    @Deprecated
    Object $genericInvoke(String methodName, String[] argTypes, Object[] args, GenericContext context);

    /**
     * 支持参数类型无法在类加载器加载情况的泛化调用
     *
     * @param methodName 调用方法名
     * @param argTypes   参数类型
     * @param args       方法参数,参数类型支持GenericObject
     * @param clazz      返回类型
     * @param context    GenericContext
     * @return 返回指定的T类型返回对象
     * @deprecated Use RpcInvokeContext instead of GenericContext
     */
    @Deprecated
    <T> T $genericInvoke(String methodName, String[] argTypes, Object[] args, Class<T> clazz, GenericContext context);
}
