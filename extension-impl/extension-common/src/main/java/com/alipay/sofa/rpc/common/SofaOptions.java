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

/**
 * Sofa Options
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaOptions {

    // ========== 一些配置的默认值 ==========
    /**
     * 默认TR协议的端口
     */
    public static final int    TR_DEFAULT_PORT                     = 12200;
    /**
     * 默认GRPC协议的端口
     *
     * @since 5.0.0
     */
    public static final int    GRPC_DEFAULT_PORT                   = 50051;

    /**
     * 最大等待时间为 30s
     */
    public static final int    DEFAULT_HEARTBEAT_INTERVAL          = 27;

    /**
     * 最大等待时间为 30s
     */
    public static final int    MAX_ADDRESS_WAIT_TIME               = 30 * 1000;

    // ========== 通用配置 ========== 
    /**
     * 应用名称
     */
    public static final String APP_NAME                            = "app_name";
    /**
     * 运行模式
     */
    public static final String CONFIG_RUN_MODE                     = "run_mode";
    /**
     * rpc_enabled_ip_range=ip1:ip2,ip3
     * ip范围在 [ip1, ip2], [ip3] 里面
     * ip可以只写部分。如XXX.XXX或者XXX
     */
    public static final String CONFIG_IP_RANGE                     = "rpc_enabled_ip_range";
    /**
     * 绑定的网卡
     */
    public static final String CONFIG_NI_BIND                      = "rpc_bind_network_interface";
    /**
     * 是否将服务提供者注册到服务注册中心（全局开关）
     *
     * @since 4.10.0
     */
    public static final String CONFIG_RPC_REGISTER_REGISTRY_IGNORE = "rpc_register_registry_ignore";
    /**
     * 虚拟主机地址，通过配置指定<br>
     * 例如宿主机地址(10.1.1.1)上有一个虚拟主机(192.2.2.2)，服务启动后监听的地址是192.2.2.2:1234，<br>
     * 但是外部系统只能通过10.1.1.1:2345访问到，所以需要告诉注册中心的地址是 10.1.1.1 <br>
     *
     * @see #CONFIG_RPC_REGISTER_VIRTUAL_PORT
     * @since 4.11.0
     */
    public static final String CONFIG_RPC_REGISTER_VIRTUAL_HOST    = "rpc_register_virtual_host";
    /**
     * 虚拟主机端口，通过配置指定<br>
     * 例如宿主机地址(10.1.1.1)上有一个虚拟主机(192.2.2.2)，服务启动后监听的地址是192.2.2.2:1234，<br>
     * 但是外部系统只能通过10.1.1.1:2345访问到，所以需要告诉注册中心的端口是 2345 <br>
     *
     * @see #CONFIG_RPC_REGISTER_VIRTUAL_HOST
     * @since 4.11.0
     */
    public static final String CONFIG_RPC_REGISTER_VIRTUAL_PORT    = "rpc_register_virtual_port";
    /**
     * 线程池是否初始化核心线程池
     *
     * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads()
     * @since 4.10.0
     */
    public static final String RPC_POOL_PRE_START                  = "rpc_pool_pre_start";

    /**
     * profile时间
     *
     * @since 5.1.0
     */
    public static final String PROFILE_THRESHOLD                   = "rpc_profile_threshold_tr";

    //========= TR 相关配置 ==========
    /**
     * TR服务监听端口
     */
    public static final String CONFIG_TR_PORT                      = "rpc_tr_port";
    /**
     * TR业务线程池的最小值
     *
     * @see #TR_MIN_POOLSIZE
     * @deprecated replace by SofaConfigs#TR_MIN_POOLSIZE
     */
    @Deprecated
    public static final String TR_MIN_POOLSIZE_OLD                 = "tr_min_pool_size";
    /**
     * TR业务线程池的最小值
     */
    public static final String TR_MIN_POOLSIZE                     = "rpc_min_pool_size_tr";
    /**
     * TR业务线程池的最大值
     *
     * @see #TR_MAX_POOLSIZE
     * @deprecated replace by SofaConfigs#TR_MAX_POOLSIZE
     */
    @Deprecated
    public static final String TR_MAX_POOLSIZE_OLD                 = "tr_max_pool_size";
    /**
     * TR业务线程池的最大值
     */
    public static final String TR_MAX_POOLSIZE                     = "rpc_max_pool_size_tr";
    /**
     * TR业务线程池的队列大小
     *
     * @see #TR_QUEUE_SIZE
     * @deprecated replace by SofaConfigs#TR_QUEUE_SIZE
     */
    @Deprecated
    public static final String TR_QUEUE_SIZE_OLD                   = "tr_queue_size";
    /**
     * TR业务线程池的队列大小
     */
    public static final String TR_QUEUE_SIZE                       = "rpc_pool_queue_size_tr";
    /**
     * TR客户端重连已断连服务端的周期（如果客户端和服务端端口连接，客户端会自动重连）
     */
    public static final String CONFIG_TR_RECONNECT_PERIOD          = "rpc_tr_reconnect_period";
    /**
     * TR长连接管理器： true:stateful/ false:stateless
     */
    public static final String CONFIG_TR_CONNECTION_HOLDER         = "rpc_tr_connection_holder";
    /**
     * 是否增加序列化安全黑名单，关闭后可提高性能
     */
    public static final String CONFIG_SERIALIZE_BLACKLIST          = "rpc_tr_serialize_blacklist";
    /**
     * 序列化覆盖
     */
    public static final String CONFIG_SERIALIZE_BLACKLIST_OVERRIDE = "rpc_serialize_blacklist_override";

    //========= GRPC 相关配置 ==========
    /**
     * gRPC服务监听端口
     */
    public static final String CONFIG_GRPC_PORT                    = "rpc_grpc_port";
    /**
     * gRPC业务线程池的最小值
     */
    public static final String GRPC_MIN_POOLSIZE                   = "rpc_min_pool_size_grpc";
    /**
     * gRPC业务线程池的最大值
     */
    public static final String GRPC_MAX_POOLSIZE                   = "rpc_max_pool_size_grpc";
    /**
     * gRPC业务线程池的队列大小
     */
    public static final String GRPC_QUEUE_SIZE                     = "rpc_pool_queue_size_grpc";

    // ========== 只能通过System.setProperty()设置，无法在文件中配置 ===========
    /**
     * 获取服务注册中心返回地址的最大等待时间  只能通过 -D 生效
     */
    public static final String CONFIG_MAX_ADDRESS_WAIT_TIME        = "rpc_max_address_wait_time";
    /**
     * TR心跳间隔
     */
    public static final String CONFIG_RPC_HEART_BEAT_INTERVAL      = "rpc_tr_heart_beat_interval";

    // ========== mesh相关 ===========

    public static final String CONFIG_RPC_MESH_SWITCH              = "rpc_mesh_switch";

}
