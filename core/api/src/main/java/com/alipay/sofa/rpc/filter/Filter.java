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

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extensible;

/**
 * Filter SPI
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class Filter {

    /**
     * Is this filter need load in this invoker
     *
     * @param invoker Filter invoker contains ProviderConfig or ConsumerConfig.
     * @return is need load
     */
    public boolean needToLoad(FilterInvoker invoker) {
        return true;
    }

    /**
     * Do filtering
     * <p>
     * <pre><code>
     *  doBeforeInvoke(); // the code before invoke, even new dummy response for return (skip all next invoke).
     *  SofaResponse response = invoker.invoke(request); // do next invoke(call next filter, call remote, call implements).
     *  doAfterInvoke(); // the code after invoke
     * </code></pre>
     *
     * @param invoker Invoker
     * @param request Request
     * @return SofaResponse Response
     * @throws SofaRpcException Occur rpc exception
     */
    public abstract SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException;

    /**
     * Do filtering after asynchronous respond, only supported in CONSUMER SIDE. <p>
     * 
     * Because when do async invoke, the code after invoke has been executed after invoker return dummy empty response.
     * We need execute filter code after get true response from server.<p>
     * 
     * NOTICE: The thread run {@link #onAsyncResponse} is different with the thread run {@link #invoke}
     *
     * @param config    ConsumerConfig, READ ONLY PLEASE.
     * @param request   Request
     * @param response  Response from server (if exception is null)
     * @param exception Exception from server (if response is null)
     * @throws SofaRpcException Other rpc exception
     * @see #invoke(FilterInvoker, SofaRequest)
     * @see SofaRequest#isAsync()
     */
    public void onAsyncResponse(ConsumerConfig config, SofaRequest request, SofaResponse response, Throwable exception)
        throws SofaRpcException {
    }
}
