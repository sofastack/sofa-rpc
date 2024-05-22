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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.common.insight.RecordContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;

import java.util.Map;

/**
 * @author Even
 * @date 2024/4/29 21:03
 */
public class RecordContextResolver {

    public static void carryWithRequest(RecordContext recordContext, SofaRequest sofaRequest) {
        recordContext.setTargetServiceUniqueName(sofaRequest.getTargetServiceUniqueName());
        recordContext.setMethodName(sofaRequest.getMethodName());
        Object traceContext = sofaRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME);
        if (traceContext instanceof Map) {
            Map<String, String> ctxMap = (Map<String, String>) traceContext;
            String traceId = ctxMap.get(RemotingConstants.TRACE_ID_KEY);
            String rpcId = ctxMap.get(RemotingConstants.RPC_ID_KEY);
            recordContext.setTraceId(traceId);
            recordContext.setRpcId(rpcId);
        }
    }
}
