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
package com.alipay.sofa.rpc.api.context;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInvokeContext;

/**
 * The util class of SOFA RPC context
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>hongwei.yhw</a>
 */
public class RpcContextManager {

    /**
     * get the last reference invoke information
     *
     * @param clear true: framework will clear the ThreadLocal when return
     * @return RPC Reference Context, it can be null
     */
    public static RpcReferenceContext lastReferenceContext(boolean clear) {
        try {
            RpcInvokeContext invokeCtx = RpcInvokeContext.getContext();
            RpcReferenceContext referenceCtx = (RpcReferenceContext) invokeCtx
                .get(RemotingConstants.INVOKE_CTX_RPC_REF_CTX);
            if (referenceCtx != null) {
                String resultCode = (String) invokeCtx.get(RemotingConstants.INVOKE_CTX_RPC_RESULT_CODE);
                if (resultCode != null) {
                    referenceCtx.setResultCode(ResultCodeEnum.getResultCode(resultCode));
                }
            }
            return referenceCtx;
        } finally {
            if (clear) {
                clearReferenceContext();
            }
        }
    }

    /**
     * get current service context
     *
     * @param clear true: framework will clear the ThreadLocal when return
     * @return RPC Service Context, it can be null
     */
    public static RpcServiceContext currentServiceContext(boolean clear) {
        try {
            RpcInvokeContext invokeCtx = RpcInvokeContext.getContext();
            return (RpcServiceContext) invokeCtx.get(RemotingConstants.INVOKE_CTX_RPC_SER_CTX);
        } finally {
            if (clear) {
                clearServiceContext();
            }
        }
    }

    public static void clearReferenceContext() {
        RpcInvokeContext.getContext().remove(RemotingConstants.INVOKE_CTX_RPC_REF_CTX);
    }

    public static void clearServiceContext() {
        RpcInvokeContext.getContext().remove(RemotingConstants.INVOKE_CTX_RPC_SER_CTX);
    }

    public static void setReferenceContext(RpcReferenceContext referenceContext) {
        RpcInvokeContext.getContext().put(RemotingConstants.INVOKE_CTX_RPC_REF_CTX, referenceContext);
    }

    public static void setServiceContext(RpcServiceContext serviceContext) {
        RpcInvokeContext.getContext().put(RemotingConstants.INVOKE_CTX_RPC_SER_CTX, serviceContext);
    }

}
