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
package com.alipay.sofa.rpc.lookout;

import com.alipay.lookout.api.Id;
import com.alipay.lookout.api.Lookout;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookoutId {

    private volatile Id  consumerId;
    private final Object consumerIdLock                  = new Object();

    private volatile Id  providerId;
    private final Object providerIdLock                  = new Object();

    private volatile Id  boltThreadPoolConfigId;
    private final Object boltThreadPoolConfigIdLock      = new Object();

    private volatile Id  boltThreadPoolActiveCountId;
    private final Object boltThreadPoolActiveCountIdLock = new Object();

    private volatile Id  boltThreadPoolIdleCountId;
    private final Object boltThreadPoolIdleCountIdLock   = new Object();

    private volatile Id  boltThreadPoolQueueSizeId;
    private final Object boltThreadPoolQueueSizeIdLock   = new Object();

    /**
     * create consumerId
     * @return consumerId
     */
    public Id getConsumerId() {

        if (consumerId == null) {
            synchronized (consumerIdLock) {
                if (consumerId == null) {
                    consumerId = Lookout.registry().createId("rpc.consumer.service.stats");
                }
            }
        }

        return consumerId;
    }

    /**
     * Create ProviderId
     * @return ProviderId
     */
    public Id getProviderId() {

        if (providerId == null) {
            synchronized (providerIdLock) {
                if (providerId == null) {
                    providerId = Lookout.registry().createId("rpc.provider.service.stats");
                }
            }
        }

        return providerId;
    }

    /**
     * Create BoltThreadPoolConfigId
     * @return BoltThreadPoolConfigId
     */
    public Id getBoltThreadPoolConfigId() {

        if (boltThreadPoolConfigId == null) {
            synchronized (boltThreadPoolConfigIdLock) {
                if (boltThreadPoolConfigId == null) {
                    boltThreadPoolConfigId = Lookout.registry().createId("rpc.bolt.threadpool.config");
                }
            }
        }

        return boltThreadPoolConfigId;
    }

    /**
     * Create BoltThreadPoolActiveCountId
     * @return BoltThreadPoolActiveCountId
     */
    public Id getBoltThreadPoolActiveCountId() {

        if (boltThreadPoolActiveCountId == null) {
            synchronized (boltThreadPoolActiveCountIdLock) {
                if (boltThreadPoolActiveCountId == null) {
                    boltThreadPoolActiveCountId = Lookout.registry().createId("rpc.bolt.threadpool.active.count");
                }
            }
        }

        return boltThreadPoolActiveCountId;
    }

    /**
     * Create BoltThreadPoolIdleCountId
     * @return BoltThreadPoolIdleCountId
     */
    public Id getBoltThreadPoolIdleCountId() {

        if (boltThreadPoolIdleCountId == null) {
            synchronized (boltThreadPoolIdleCountIdLock) {
                if (boltThreadPoolIdleCountId == null) {
                    boltThreadPoolIdleCountId = Lookout.registry().createId("rpc.bolt.threadpool.idle.count");
                }
            }
        }

        return boltThreadPoolIdleCountId;
    }

    /**
     * Create BoltThreadPoolQueueSizeId
     * @return BoltThreadPoolQueueSizeId
     */
    public Id getBoltThreadPoolQueueSizeId() {

        if (boltThreadPoolQueueSizeId == null) {
            synchronized (boltThreadPoolQueueSizeIdLock) {
                if (boltThreadPoolQueueSizeId == null) {
                    boltThreadPoolQueueSizeId = Lookout.registry().createId("rpc.bolt.threadpool.queue.size");
                }
            }
        }

        return boltThreadPoolQueueSizeId;
    }
}