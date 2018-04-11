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
package com.alipay.sofa.rpc.core.exception;

/**
 * SOFA RPC Exception, all rpc exception will extends it
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaRpcException extends RuntimeException {

    /**
     * 异常类型
     */
    protected int errorType = RpcErrorType.UNKNOWN;

    protected SofaRpcException() {

    }

    public SofaRpcException(int errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public SofaRpcException(int errorType, Throwable cause) {
        super(cause);
        this.errorType = errorType;
    }

    public SofaRpcException(int errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public int getErrorType() {
        return errorType;
    }

    @Deprecated
    public SofaRpcException(String message) {
        super(message);
    }

    @Deprecated
    public SofaRpcException(String message, Throwable t) {
        super(message, t);
    }
}
