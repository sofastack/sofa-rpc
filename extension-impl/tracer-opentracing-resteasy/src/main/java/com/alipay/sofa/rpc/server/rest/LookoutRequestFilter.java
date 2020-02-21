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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.lookout.RestConstants;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_PREFIX;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">liangen</a>
 */
@Provider
@Priority(100)
public class LookoutRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LookoutRequestFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        try {
            SofaResourceMethodInvoker resourceMethodInvoker = (SofaResourceMethodInvoker)
                    ((PostMatchContainerRequestContext) requestContext)
                        .getResourceMethod();

            SofaResourceFactory factory = resourceMethodInvoker.getResource();
            String serviceName = factory.getServiceName();
            String appName = factory.getAppName();

            if (serviceName == null) {
                serviceName = resourceMethodInvoker.getResourceClass().getName();
            }

            String methodName = resourceMethodInvoker.getMethod().getName();

            RpcInternalContext context = RpcInternalContext.getContext();
            context.setAttachment(INTERNAL_KEY_PREFIX + RestConstants.REST_SERVICE_KEY, serviceName);
            context.setAttachment(INTERNAL_KEY_PREFIX + RestConstants.REST_METHODNAME_KEY, methodName);

            context.setAttachment(RemotingConstants.HEAD_APP_NAME, appName);
        } catch (Exception e) {
            logger.error(LogCodes.getLog(LogCodes.ERROR_LOOKOUT_PROCESS), e);
        }

    }
}
