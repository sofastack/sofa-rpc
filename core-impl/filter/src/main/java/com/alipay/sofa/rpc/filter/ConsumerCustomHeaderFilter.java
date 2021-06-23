/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.common.utils.Ordered;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.Map;

/**
 * Set customHeader to  SofaRequest.requestProps
 *
 * @author zhaowang
 * @version : ConsumerCustomHeaderFilter.java, v 0.1 2021年06月23日 2:05 下午 zhaowang
 */
@AutoActive(consumerSide = true)
@Extension(value = "consumerException",order = Ordered.LOWEST_PRECEDENCE)
public class ConsumerCustomHeaderFilter extends Filter{


    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        setCustomHeader(request);
        return invoker.invoke(request);
    }

    private void setCustomHeader(SofaRequest sofaRequest) {
        RpcInternalContext context = RpcInternalContext.getContext();
        Map customHeader = context.getCustomHeader();
        if (CommonUtils.isNotEmpty(customHeader)) {
            sofaRequest.addRequestProps(customHeader);
        }
        context.clearCustomHeader();
    }
}