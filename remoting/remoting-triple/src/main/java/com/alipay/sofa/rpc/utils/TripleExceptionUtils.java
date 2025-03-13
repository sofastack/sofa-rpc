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
package com.alipay.sofa.rpc.utils;

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class TripleExceptionUtils {

    public static StatusRuntimeException asStatusRuntimeException(Throwable t) {
        if (t != null) {
            return Status.fromThrowable(t).withDescription(t.getMessage()).withCause(t.getCause()).asRuntimeException();
        } else {
            return Status.UNKNOWN.withDescription("Error message is null.").asRuntimeException();
        }
    }

    public static Throwable getThrowableFromStatus(Status status) {
        if (status.getCode() == Status.OK.getCode()) {
            return null;
        } else if (status.getCode() == Status.UNAVAILABLE.getCode()) {
            return new SofaRouteException(status.getDescription(), status.getCause());
        } else if (status.getCode() == Status.DEADLINE_EXCEEDED.getCode()) {
            return new SofaTimeOutException(status.getDescription(), status.getCause());
        } else {
            return new SofaRpcException(RpcErrorType.UNKNOWN, status.getDescription(), status.getCause());
        }
    }

}
