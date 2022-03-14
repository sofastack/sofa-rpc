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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;

/**
 * 只是Bolt Remoting 层的一些常量。 非全局公共，所以不放在SofaConstants里面
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RemotingConstants {

    /**
     * 协议：tr，老协议
     * // com.taobao.remoting.TRConstants#PROCOCOL_VERSION;
     */
    public static final byte   PROTOCOL_TR                = 13;
    /**
     * 协议：bolt
     * // RpcProtocol.PROTOCOL_CODE;
     */
    public static final byte   PROTOCOL_BOLT              = 1;

    /*
    将TrConstant的序列化   com.taobao.remoting.TRConstants
        static public final byte HESSIAN_SERIALIZE = 1; // HESSIAN序列化
        static public final byte JAVA_SERIALIZE = 2;    // Java序列化
        static public final byte TOP_SERIALIZE = 3;     // TOP协议格式
        static public final byte HESSIAN2_SERIALIZE = 4;// HESSIAN2序列化
     映射到 bolt的序列化   com.alipay.sofa.rpc.core.util.RpcConstants
        public static final byte   SERIALIZE_CODE_HESSIAN = 1
        public static final byte   SERIALIZE_CODE_JAVA     = 2;
        public static final byte   SERIALIZE_CODE_PROTOBUF = 11;
     */

    /**
     * hessian/hessian2  对应bolt固定同步里的codec字段
     *
     * @see com.alipay.sofa.rpc.common.RpcConstants#SERIALIZE_HESSIAN
     * @see com.alipay.sofa.rpc.common.RpcConstants#SERIALIZE_HESSIAN2
     */
    public static final byte   SERIALIZE_CODE_HESSIAN     = 1;

    /**
     * java  对应bolt固定同步里的codec字段
     *
     * @see com.alipay.sofa.rpc.common.RpcConstants#SERIALIZE_JAVA
     */
    public static final byte   SERIALIZE_CODE_JAVA        = 2;

    /**
     * hessian2  对应bolt固定同步里的codec字段
     *
     * @see com.alipay.sofa.rpc.common.RpcConstants#SERIALIZE_HESSIAN2
     * @deprecated use {@link #SERIALIZE_CODE_HESSIAN}
     */
    @Deprecated
    public static final byte   SERIALIZE_CODE_HESSIAN2    = 4;

    /**
     * protobuf  对应bolt固定同步里的codec字段
     *
     * @see com.alipay.sofa.rpc.common.RpcConstants#SERIALIZE_PROTOBUF
     */
    public static final byte   SERIALIZE_CODE_PROTOBUF    = 11;

    /**
     * 普通序列化：序列化反序列化均使用SofaSerializerFactory
     */
    public static final String SERIALIZE_FACTORY_NORMAL   = "0";

    /**
     * 混合序列化：序列化使用SofaGenericSerializerFactory, 反序列化使用SofaSerializerFactory
     */
    public static final String SERIALIZE_FACTORY_MIX      = "1";

    /**
     * 泛型序列化：序列化反序列化均使用SofaGenericSerializerFactory
     */
    public static final String SERIALIZE_FACTORY_GENERIC  = "2";

    //========= tracer 相关 ===========
    /**
     * 老 Trace上下文
     */
    public static final String RPC_TRACE_NAME             = "rpc_trace_context";

    /***
     * 新 tracer 需要在服务端和客户端缓存数据,用此 key 标示在请求头中的数据
     */
    public static final String NEW_RPC_TRACE_NAME         = "new_rpc_trace_context";

    /***
     * 新 tracer 需要在服务端和客户端缓存数据,用此 key 标示在请求头中的数据
     */
    public static final String HTTP_HEADER_TRACE_ID_KEY   = "SOFA-TraceId";

    /***
     * 新 tracer 需要在服务端和客户端缓存数据,用此 key 标示在请求头中的数据
     */
    public static final String HTTP_HEADER_RPC_ID_KEY     = "SOFA-RpcId";

    /** TraceId 放在透传上下文中的 key */
    public static final String TRACE_ID_KEY               = "sofaTraceId";

    /** RpcId 放在透传上下文中的 key */
    public static final String RPC_ID_KEY                 = "sofaRpcId";

    /** penetrateAttributes 放在透传上下文中的 key */
    public static final String PEN_ATTRS_KEY              = "sofaPenAttrs";

    // ============ 序列化相关 ===========
    /**
     * 对方服务名
     */
    public static final String HEAD_SERVICE               = "service";
    /**
     * 客户端的调用类型
     */
    public static final String HEAD_INVOKE_TYPE           = "type";
    /**
     * 客户端应用
     */
    public static final String HEAD_APP_NAME              = "app";
    /**
     * 客户端应用
     */
    public static final String HEAD_PROTOCOL              = "protocol";

    /**
     * 忽略浏览器的图标请求
     */
    public static final String IGNORE_WEB_BROWSER         = "/favicon.ico";

    // ========== 头相关 ============
    /**
     * 对方方法名
     *
     * @see SofaRequest#methodName
     * @since 5.1.0
     */
    public static final String HEAD_METHOD_NAME           = "sofa_head_method_name";
    /**
     * 对应 SofaRequest#targetAppName
     *
     * @see SofaRequest#targetAppName
     * @since 5.1.0
     */
    public static final String HEAD_TARGET_APP            = "sofa_head_target_app";
    /**
     * 对应 RequestBase#targetServiceUniqueName
     *
     * @see com.alipay.sofa.rpc.core.request.RequestBase#targetServiceUniqueName
     * @since 5.1.0
     */
    public static final String HEAD_TARGET_SERVICE        = "sofa_head_target_service";
    /**
     * 对应 RequestBase#methodArgSigs
     *
     * @see com.alipay.sofa.rpc.core.request.RequestBase#methodArgSigs
     * @since 5.1.0
     */
    @Deprecated
    public static final String HEAD_METHOD_ARGSIGS        = "sofa_head_method_argsigs";
    /**
     * 对应 SofaRequest#requestProps
     *
     * @see SofaRequest#requestProps
     * @since 5.1.0
     */
    @Deprecated
    public static final String HEAD_REQUEST_PROPS         = "sofa_head_request_props";

    /**
     * 对应 SofaResponse#isError
     *
     * @see SofaResponse#isError
     * @since 5.1.0
     */
    public static final String HEAD_RESPONSE_ERROR        = "sofa_head_response_error";
    /**
     * 是否泛化调用
     *
     * @since 5.4.0
     */
    public static final String HEAD_GENERIC_TYPE          = "sofa_head_generic_type";
    /**
     *
     * @since 5.4.0
     */
    public static final String HEAD_SERIALIZE_TYPE        = "sofa_head_serialize_type";

    /**
     * RPC透传请求链路数据
     *
     * @since 4.12.0
     */
    public static final String RPC_REQUEST_BAGGAGE        = "rpc_req_baggage";

    /**
     * RPC响应链路透传数据
     *
     * @since 4.12.0
     */
    public static final String RPC_RESPONSE_BAGGAGE       = "rpc_resp_baggage";

    // =========RpcInvokeContext的Key========
    /**
     * bolt RpcInvokeContext的Key
     *
     * @since 5.1.0
     */
    public static final String INVOKE_CTX_RPC_CTX         = "rpc.ctx";

    /**
     * bolt RpcInvokeContext的Key
     *
     * @since 5.1.1
     */
    public static final String INVOKE_CTX_RPC_SER_CTX     = "rpc.service.ctx";
    /**
     * bolt RpcInvokeContext的Key
     *
     * @since 5.1.1
     */
    public static final String INVOKE_CTX_RPC_REF_CTX     = "rpc.reference.ctx";
    /**
     * bolt RpcInvokeContext的Key
     *
     * @since 5.1.1
     */
    public static final String INVOKE_CTX_RPC_RESULT_CODE = "rpc.result.code";

    /**
     * bolt RpcInvokeContext的Key
     *
     * @since 5.1.0
     */
    public static final String INVOKE_CTX_IS_ASYNC_CHAIN  = "rpc.async.chain";
}