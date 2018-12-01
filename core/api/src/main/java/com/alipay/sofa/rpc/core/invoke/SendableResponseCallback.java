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

/**
 * 表示是一个异步可链路返回的Callback
 * <p>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public interface SendableResponseCallback<T> extends SofaResponseCallback<T> {

    /**
     * A-&gt;B(当前)-&gt;C的场景下，B将异常异步返回给调用者A
     *
     * @param appResponse 返回给A的值
     */
    void sendAppResponse(Object appResponse);

    /**
     * A-&gt;B(当前)-&gt;C的场景下，B将异常异步返回给调用者A
     *
     * @param throwable 返回给A的异常
     */
    void sendAppException(Throwable throwable);

    /**
     * A-&gt;B(当前)-&gt;C的场景下，B将异常异步返回给调用者A
     *
     * @param exception 返回给A的异常
     */
    void sendSofaException(SofaRpcException exception);
}
