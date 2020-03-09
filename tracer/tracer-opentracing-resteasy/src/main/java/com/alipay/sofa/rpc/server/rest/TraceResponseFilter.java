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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author <a href="mailto:lw111072@alibaba-inc.com">liangen</a>
 */
@Priority(100)
@Provider
public class TraceResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {

        // 补充服务端request和response大小
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext context = RpcInternalContext.getContext();
            context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, requestContext.getLength());
            context.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, responseContext.getLength());
            Long startTime = (Long) context.removeAttachment(RpcConstants.INTERNAL_KEY_SERVER_RECEIVE_TIME);
            if (startTime != null) {
                context.setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE, RpcRuntimeContext.now() - startTime);
            }
        }
    }
}
