/*-
 * #%L
 * sofa-rpc-core
 * %%
 * Copyright (C) 2016 - 2021 Ant Financial Services Group
 * %%
 * This software is developed by Ant Financial Services Group.This software and all the relevant information, 
 * including but not limited to any signs, images, photographs, animations, text, interface design, 
 * audios and videos, and printed materials, are protected by copyright laws and other intellectual property laws and treaties. 
 * The use of this software shall abide by the laws and regulations as well as Software Installation License Agreement/Software 
 * Use Agreement updated from time to time. Without authorization from Ant Financial Services Group , 
 * no one may conduct the following actions: 
 * 
 * 1) reproduce, spread, present, set up a mirror of, upload, download this software; 
 * 
 * 2) reverse engineer, decompile the source code of this software or try to find the source code in any other ways; 
 * 
 * 3) modify, translate and adapt this software, or develop derivative products, works, and services based on this software; 
 * 
 * 4) distribute, lease, rent, sub-license, demise or transfer any rights in relation to this software, 
 * or authorize the reproduction of this software on other computers.
 * #L%
 */

/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.alipay.sofa.rpc.event;

/**
 * RPC 处理的不同阶段, 具体含义可以参考文档: https://yuque.antfin-inc.com/middleware/sofa-rpc/uvc8ol
 *
 *
 * @author zhaowang
 * @version : LifeCyclePhase.java, v 0.1 2021年06月29日 6:54 下午 zhaowang
 */
public enum Phase {

    // 序列化相关
    REQUEST_SER, REQUEST_DES, RESPONSE_SER, RESPONSE_DES,

    REQUEST_HEADER_SER, REQUEST_HEADER_DES, RESPONSE_HEADER_SER, RESPONSE_HEADER_DES,

    // 客户端阶段

    /**
     * 包含客户端完整的调用时间
     */
    CLIENT_ALL,
    /**
     * 路由阶段
     */
    CLIENT_ROUTE,
    /**
     * 建立连接阶段
     */
    CLIENT_CONN,
    /**
     * 过滤器阶段
     */
    CLIENT_FILTER,
    /**
     * 负载均衡阶段
     */
    CLIENT_LB,
    /**
     * ambush 执行阶段,时间包含了请求服务端
     */
    CLIENT_AMBUSH,
    /**
     * ambush 执行完成
     */
    CLIENT_IN_AMBUSH,
    /**
     * RPC 客户端处理完成,交给 bolt client 进行调用的时间
     */
    CLIENT_INVOKE,

    // server
    /**
     * 网络等待阶段
     * 从 'Bolt 定长 Header 可以解析' 到 '接收到完整 Bolt 请求' 的时间
     */
    SERVER_NET_WAIT,
    /**
     * 线程等待阶段
     * 从任务加入线程池到从线程池取出开始执行
     */
    SERVER_THREAD_WAIT,
    /**
     * 业务等待阶段
     * 从 'Bolt 定长 Header 可以解析' 到 '从线程池取出开始执行' 的时间
     */
    SERVER_BIZ_WAIT,
    /**
     * filter 执行阶段
     */
    SERVER_FILTER,
    /**
     * ambush 执行阶段,包含业务执行时间
     */
    SERVER_AMBUSH,
    /**
     * 业务执行阶段
     * 真正开始执行业务代码
     */
    SERVER_BIZ,
    /**
     * RPC Bolt 处理器处理阶段
     * 包含完整的 RPC 处理流程
     */
    SERVER_BOLT_HANDLE_REQUEST,
}