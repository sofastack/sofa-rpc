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
package com.alipay.sofa.rpc.core.invoke;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.RequestBase;

/**
 * 面向用户的Rpc请求结果监听器
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface SofaResponseCallback<T> {
    /**
     * SOFA RPC will callback this method when server return response success
     *
     * @param appResponse response object
     * @param methodName the invoked method
     * @param request the invoked request object
     */
    void onAppResponse(Object appResponse, String methodName, RequestBase request);

    /**
     * SOFA RPC will callback this method when server meet exception
     *
     * @param throwable app's exception
     * @param methodName the invoked method
     * @param request the invoked request
     */
    void onAppException(Throwable throwable, String methodName, RequestBase request);

    /**
     * SOFA RPC will callback this method when framework meet exception
     *
     * @param sofaException framework exception
     * @param methodName the invoked method
     * @param request the invoked request object
     */
    void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request);
}
