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
 * RPC 错误类型
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcErrorType {

    public static final int UNKNOWN                  = 0;

    // ==== 服务端异常码 100-199 =======

    /**
     * 服务端忙异常
     */
    public static final int SERVER_BUSY              = 100;
    /**
     * 服务端已关闭
     */
    public static final int SERVER_CLOSED            = 101;
    /**
     * 服务端未找到Invoker
     */
    public static final int SERVER_NOT_FOUND_INVOKER = 110;
    /**
     * 服务端序列化异常
     */
    public static final int SERVER_SERIALIZE         = 120;
    /**
     * 服务端反序列化异常
     */
    public static final int SERVER_DESERIALIZE       = 130;

    /**
     * 服务端网络异常（服务端往回发的时候）
     */
    public static final int SERVER_NETWORK           = 150;

    /**
     * 服务端业务异常
     */
    public static final int SERVER_BIZ               = 160;

    /**
     * 服务端过滤器异常
     */
    public static final int SERVER_FILTER            = 170;

    /**
     * 服务端未定义异常，注意：对于客户端来说，服务端异常只分为业务异常和服务端异常，不知道服务端异常的明细
     */
    public static final int SERVER_UNDECLARED_ERROR  = 199;

    // ==== 客户端异常码 200-299 ======
    /**
     * 客户端超时异常
     */
    public static final int CLIENT_TIMEOUT           = 200;

    /**
     * 客户端路由寻址异常
     */
    public static final int CLIENT_ROUTER            = 210;

    /**
     * 客户端序列化异常
     */
    public static final int CLIENT_SERIALIZE         = 220;
    /**
     * 客户端反序列化异常
     */
    public static final int CLIENT_DESERIALIZE       = 230;

    /**
     * 客户端网络异常（客户端往外发的时候）
     */
    public static final int CLIENT_NETWORK           = 250;

    /**
     * 客户端过滤器异常
     */
    public static final int CLIENT_FILTER            = 270;

    /**
     * 客户端未定义异常
     */
    public static final int CLIENT_UNDECLARED_ERROR  = 299;
}
