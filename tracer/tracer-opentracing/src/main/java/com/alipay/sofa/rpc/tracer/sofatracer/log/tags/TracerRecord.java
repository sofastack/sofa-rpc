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
 * 阶段说明	                        TraceLog字符串	备注
 * 路由寻址（client）	                R1
 * 建立链接（client） 	            R2
 * Filter过滤（client）	            R3
 * 负载均衡LB（client）	            R4
 * 请求序列化/响应反序列化（client） 	R5
 * 请求反序列化/响应序列化（server）	R6
 * 线程等待（server）             	R7
 * 业务执行时间（server）	            R8
 * ambush 耗时	                    R9
 * Filter过滤（server）              R10
 * @author zhaowang
 * @version : TracerRecord.java, v 0.1 2021年06月29日 8:20 下午 zhaowang
 */
public enum TracerRecord {
    R1, R2, R3, R4, R5, R6, R7, R8, R9, R10
}