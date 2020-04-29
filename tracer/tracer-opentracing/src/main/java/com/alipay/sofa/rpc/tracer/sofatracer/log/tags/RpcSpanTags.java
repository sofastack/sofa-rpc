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
package com.alipay.sofa.rpc.tracer.sofatracer.log.tags;

/**
 * RpcSpanTags
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcSpanTags {

    //************** String 类型

    public static final String TRACERID                     = "tracerId";

    public static final String SPANID                       = "spanId";

    public static final String BAGGAGE                      = "baggage";

    public static final String TIMESTAMP                    = "timestamp";

    public static final String LOCAL_APP                    = "local.app";

    public static final String SERVICE                      = "service";

    public static final String METHOD                       = "method";

    public static final String PROTOCOL                     = "protocol";

    public static final String INVOKE_TYPE                  = "invoke.type";

    /***
     * 对端地址:目标、来源（相对当前而言）
     */
    public static final String REMOTE_IP                    = "remote.ip";

    /***
     * 对端应用
     */
    public static final String REMOTE_APP                   = "remote.app";

    /***
     * 对端 zone
     */
    public static final String REMOTE_ZONE                  = "remote.zone";

    /***
     * 对端 idc
     */
    public static final String REMOTE_IDC                   = "remote.idc";

    /***
     * 对端 city
     */
    public static final String REMOTE_CITY                  = "remote.city";

    /***
     * userId
     */
    public static final String USER_ID                      = "user.id";

    /**
     * 结果码:00=成功/01=业务异常/02=RPC逻辑错误/03=超时失败/04=路由失败
     */
    public static final String RESULT_CODE                  = "result.code";

    /***
     * 当前线程名字
     */
    public static final String CURRENT_THREAD_NAME          = "current.thread.name";

    /**
     * 路由记录
     */
    public static final String ROUTE_RECORD                 = "router.record";

    /***
     * 当前客户端ip地址
     */
    public static final String LOCAL_IP                     = "local.client.ip";

    /***
     * 当前客户端端口
     */
    public static final String LOCAL_PORT                   = "local.client.port";

    //************************* Number 类型 ****************************
    /***
     * 业务处理时间
     */
    public static final String SERVER_BIZ_TIME              = "biz.impl.time";

    /***
     * 请求大小
     */
    public static final String REQ_SIZE                     = "req.size";

    /***
     * 响应大小
     */
    public static final String RESP_SIZE                    = "resp.size";

    /***
     * 客户端创建长连接时间
     */
    public static final String CLIENT_CONN_TIME             = "client.conn.time";

    /***
     * 客户端总耗时
     */
    public static final String CLIENT_ELAPSE_TIME           = "client.elapse.time";

    /***
     * 客户端请求序列化时间
     */
    public static final String REQ_SERIALIZE_TIME           = "req.serialize.time";

    /***
     * 服务端响应序列化耗时
     */
    public static final String RESP_SERIALIZE_TIME          = "resp.serialize.time";

    /***
     * 服务端请求序列化耗时
     */
    public static final String REQ_DESERIALIZE_TIME         = "req.deserialize.time";

    /***
     * 客户端接收响应反序列化时间
     */
    public static final String RESP_DESERIALIZE_TIME        = "resp.deserialize.time";

    /***
     * 服务端线程池等待时间
     */
    public static final String SERVER_THREAD_POOL_WAIT_TIME = "server.pool.wait.time";

}
