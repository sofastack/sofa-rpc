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
package com.alipay.sofa.rpc.transport.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.tracer.sofatracer.RestTracerAdapter;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 * @author <a href="mailto:lw111072@alibaba-inc.com">liangen</a>
 */
@Provider
@Priority(100)
public class TraceClientRequestFilter implements ClientRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TraceClientRequestFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        try {

            if (RpcInternalContext.isAttachmentEnable()) {
                // 补充客户端request长度
                RpcInternalContext context = RpcInternalContext.getContext();
                context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE,
                    requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH));

            }

            RestTracerAdapter.beforeSend(requestContext);
        } catch (Exception e) {
            logger.error(LogCodes.getLog(LogCodes.ERROR_TRACER_UNKNOWN_EXP, "filter", "rest", "client"), e);
        }
    }
}
