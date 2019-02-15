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
package com.alipay.sofa.rpc.lookout;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.rest.RestServerSendEvent;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_PREFIX;

/**
 * @author bystander
 * @version $Id: RestLookoutAdapter.java, v 0.1 2019年01月28日 16:44 bystander Exp $
 */
public class RestLookoutAdapter {

    public static void sendRestServerSendEvent(RestServerSendEvent restServerSendEvent) {
        //this is special for rest
        if (EventBus.isEnable(ServerSendEvent.class)) {
            SofaRequest request = new SofaRequest();

            String appName = (String) RpcRuntimeContext.get(RpcRuntimeContext.KEY_APPNAME);
            request.setTargetAppName(appName);
            request.addRequestProp(RemotingConstants.HEAD_APP_NAME, restServerSendEvent.getRequest().getHttpHeaders()
                .getHeaderString(RemotingConstants.HEAD_APP_NAME));
            RpcInternalContext context = RpcInternalContext.getContext();
            request.setTargetServiceUniqueName((String) context.getAttachment(INTERNAL_KEY_PREFIX +
                RestConstants.REST_SERVICE_KEY));

            request.setMethodName((String) context.getAttachment(INTERNAL_KEY_PREFIX +
                RestConstants.REST_METHODNAME_KEY));
            request.addRequestProp(RemotingConstants.HEAD_PROTOCOL, RpcConstants.PROTOCOL_TYPE_REST);
            request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
            SofaResponse response = new SofaResponse();

            if (restServerSendEvent.getThrowable() != null) {
                response.setErrorMsg(restServerSendEvent.getThrowable().getMessage());
            }
            final ServerSendEvent event = new ServerSendEvent(request, response, restServerSendEvent.getThrowable());
            EventBus.post(event);
        }
    }
}