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
package com.alipay.sofa.rpc.client;

/**
 * 服务提供者的状态
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.2.0
 */
public enum ProviderStatus {
    /**
     * 可用
     */
    AVAILABLE,

    /**
     * 禁用
     */
    DISABLED,

    /**
     * 预热中
     */
    WARMING_UP,

    /**
     * 服务器繁忙
     */
    BUSY,

    /**
     * 被降级
     */
    DEGRADED,

    /**
     * 恢复中
     */
    RECOVERING,

    /**
     * 服务器暂停
     */
    PAUSED,

    /**
     * 服务端即将关闭
     */
    PRE_CLOSE
}
