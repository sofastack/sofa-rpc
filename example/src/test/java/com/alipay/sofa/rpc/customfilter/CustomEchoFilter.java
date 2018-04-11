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
package com.alipay.sofa.rpc.customfilter;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("customEcho")
public class CustomEchoFilter extends Filter {

    /**
     * Logger for CustomEchoFilter
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomEchoFilter.class);

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        // 判断一些条件，自己决定是否加载这个Filter
        return true;
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        LOGGER.info("echo request : {}, {}", request.getInterfaceName() + "." + request.getMethodName(),
            request.getMethodArgs());

        SofaResponse response = invoker.invoke(request);

        if (response == null) {
            return response;
        } else if (response.isError()) {
            LOGGER.info("server rpc error: {}", response.getErrorMsg());
        } else {
            Object ret = response.getAppResponse();
            if (ret instanceof Throwable) {
                LOGGER.error("server biz error: {}", (Throwable) ret);
            } else {
                LOGGER.info("echo response : {}", response.getAppResponse());
            }
        }

        return response;
    }
}
