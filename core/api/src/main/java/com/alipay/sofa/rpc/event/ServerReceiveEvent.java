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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;

/**
 * ServerReceiveEvent
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerReceiveEvent implements Event {
    private final SofaRequest request;

    public ServerReceiveEvent(SofaRequest request) {
        this.request = request;
        // S1:The time when the server receives the first packet, in microseconds
        RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_SERVER_RECEIVE_TIME_MICRO, RpcRuntimeContext.now());
    }

    public SofaRequest getRequest() {
        return request;
    }
}
