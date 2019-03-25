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
package com.alipay.sofa.rpc.test.client;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;

/**
 * @author bystander
 * @version : BoltServerMockProcesser.java, v 0.1 2019年03月23日 16:57 bystander Exp $
 */
public class BoltServerMockProcesser extends AsyncUserProcessor<SofaRequest> {

    private RpcServer rpcServer;

    private boolean   reversed;

    public BoltServerMockProcesser(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, SofaRequest request) {

        InvokeContext invokeContext = new InvokeContext();
        invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, Byte.valueOf("1"));
        invokeContext.put(RemotingConstants.HEAD_TARGET_SERVICE, request.getTargetServiceUniqueName());
        invokeContext.put(RemotingConstants.HEAD_METHOD_NAME, request.getMethodName());
        String genericType = (String) request.getRequestProp(RemotingConstants.HEAD_GENERIC_TYPE);

        if (genericType != null) {
            invokeContext.put(RemotingConstants.HEAD_GENERIC_TYPE, genericType);
        }
        Object result = null;
        try {
            result = rpcServer.invokeSync(bizCtx.getRemoteAddress(), request, invokeContext, 3000);
        } catch (Throwable e) {
            e.printStackTrace();
            reversed = false;
        } finally {
            System.out.println("test final" + result);
            reversed = true;
        }

    }

    @Override
    public String interest() {
        return SofaRequest.class.getName();
    }

    public boolean isReversed() {
        return reversed;
    }
}