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
import com.alipay.sofa.rpc.ext.Extension;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "testChainFilter1", order = 1)
public class TestChainFilter1 extends Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return false;
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        request.getMethodArgs()[0] = request.getMethodArgs()[0] + "_q1";
        SofaResponse response = invoker.invoke(request);
        if (!request.isAsync()) {
            response.setAppResponse(response.getAppResponse() + "_s1");
        }
        return response;
    }

    @Override
    public void onAsyncResponse(ConsumerConfig config, SofaRequest request, SofaResponse response, Throwable throwable)
        throws SofaRpcException {
        response.setAppResponse(response.getAppResponse() + "_a1");
    }
}
