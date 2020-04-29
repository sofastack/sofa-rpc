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
package com.alipay.sofa.rpc.metrics.lookout;

import com.alipay.lookout.api.Counter;
import com.alipay.lookout.api.DistributionSummary;
import com.alipay.lookout.api.Gauge;
import com.alipay.lookout.api.Id;
import com.alipay.lookout.api.Lookout;
import com.alipay.lookout.api.Timer;
import com.alipay.lookout.api.composite.MixinMetric;
import com.alipay.lookout.api.info.Info;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Rpc reports the information to lookout.
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookout {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER       = LoggerFactory.getLogger(RpcLookout.class);

    private final RpcLookoutId  rpcLookoutId = new RpcLookoutId();

    /**
     * Collect the RPC client information.
     *
     * @param rpcClientMetricsModel client information model
     */
    public void collectClient(RpcClientLookoutModel rpcClientMetricsModel) {

        try {
            Id methodConsumerId = createMethodConsumerId(rpcClientMetricsModel);
            MixinMetric methodConsumerMetric = Lookout.registry().mixinMetric(methodConsumerId);

            recordCounterAndTimer(methodConsumerMetric, rpcClientMetricsModel);

            recordSize(methodConsumerMetric, rpcClientMetricsModel);

        } catch (Throwable t) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_METRIC_REPORT_ERROR), t);
        }
    }

    /**
     * Collect the RPC server information.
     *
     * @param rpcServerMetricsModel server information model
     */
    public void collectServer(RpcServerLookoutModel rpcServerMetricsModel) {

        try {
            Id methodProviderId = createMethodProviderId(rpcServerMetricsModel);
            MixinMetric methodProviderMetric = Lookout.registry().mixinMetric(methodProviderId);

            recordCounterAndTimer(methodProviderMetric, rpcServerMetricsModel);

        } catch (Throwable t) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_METRIC_REPORT_ERROR), t);
        }
    }

    /**
     * Collect the thread pool information
     *
     * @param serverConfig       ServerConfig
     * @param threadPoolExecutor ThreadPoolExecutor
     */
    public void collectThreadPool(ServerConfig serverConfig, final ThreadPoolExecutor threadPoolExecutor) {
        try {

            int coreSize = serverConfig.getCoreThreads();
            int maxSize = serverConfig.getMaxThreads();
            int queueSize = serverConfig.getQueues();

            final ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig(coreSize, maxSize, queueSize);

            Lookout.registry().info(rpcLookoutId.fetchServerThreadConfigId(serverConfig), new Info<ThreadPoolConfig>() {

                @Override
                public ThreadPoolConfig value() {
                    return threadPoolConfig;
                }
            });

            Lookout.registry().gauge(rpcLookoutId.fetchServerThreadPoolActiveCountId(serverConfig),
                new Gauge<Integer>() {

                    @Override
                    public Integer value() {
                        return threadPoolExecutor.getActiveCount();
                    }
                });

            Lookout.registry().gauge(rpcLookoutId.fetchServerThreadPoolIdleCountId(serverConfig), new Gauge<Integer>() {

                @Override
                public Integer value() {
                    return threadPoolExecutor.getPoolSize() - threadPoolExecutor.getActiveCount();
                }
            });

            Lookout.registry().gauge(rpcLookoutId.fetchServerThreadPoolQueueSizeId(serverConfig), new Gauge<Integer>() {

                @Override
                public Integer value() {
                    return threadPoolExecutor.getQueue().size();
                }
            });
        } catch (Throwable t) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_METRIC_REPORT_ERROR), t);
        }
    }

    /**
     * remove the thread pool information
     *
     * @param serverConfig server config
     */
    public void removeThreadPool(ServerConfig serverConfig) {
        Lookout.registry().removeMetric(rpcLookoutId.removeServerThreadConfigId(serverConfig));
        Lookout.registry().removeMetric(rpcLookoutId.removeServerThreadPoolActiveCountId(serverConfig));
        Lookout.registry().removeMetric(rpcLookoutId.removeServerThreadPoolIdleCountId(serverConfig));
        Lookout.registry().removeMetric(rpcLookoutId.removeServerThreadPoolQueueSizeId(serverConfig));
    }

    /**
     * Record the number of calls and time consuming.
     *
     * @param mixinMetric MixinMetric
     * @param model       information model
     */
    private void recordCounterAndTimer(MixinMetric mixinMetric, RpcAbstractLookoutModel model) {
        Counter totalCounter = mixinMetric.counter("total_count");
        Timer totalTimer = mixinMetric.timer("total_time");

        Long elapsedTime = model.getElapsedTime();

        totalCounter.inc();
        if (elapsedTime != null) {
            totalTimer.record(elapsedTime, TimeUnit.MILLISECONDS);
        }

        if (!model.getSuccess()) {
            Counter failCounter = mixinMetric.counter("fail_count");
            Timer failTimer = mixinMetric.timer("fail_time");

            failCounter.inc();
            if (elapsedTime != null) {
                failTimer.record(elapsedTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Record request size and response size
     *
     * @param mixinMetric MixinMetric
     * @param model       information model
     */
    private void recordSize(MixinMetric mixinMetric, RpcClientLookoutModel model) {

        Long requestSize = model.getRequestSize();
        Long responseSize = model.getResponseSize();

        if (requestSize != null) {
            DistributionSummary requestSizeDS = mixinMetric.distributionSummary("request_size");
            requestSizeDS.record(model.getRequestSize());
        }

        if (responseSize != null) {
            DistributionSummary responseSizeDS = mixinMetric.distributionSummary("response_size");
            responseSizeDS.record(model.getResponseSize());
        }
    }

    /**
     * Create consumer id
     *
     * @param model RpcClientLookoutModel
     * @return Id
     */
    private Id createMethodConsumerId(RpcClientLookoutModel model) {

        Map<String, String> tags = new HashMap<String, String>(6);

        tags.put("app", StringUtils.defaultString(model.getApp()));
        tags.put("service", StringUtils.defaultString(model.getService()));
        tags.put("method", StringUtils.defaultString(model.getMethod()));
        tags.put("protocol", StringUtils.defaultString(model.getProtocol()));
        tags.put("invoke_type", StringUtils.defaultString(model.getInvokeType()));
        tags.put("target_app", StringUtils.defaultString(model.getTargetApp()));

        return rpcLookoutId.fetchConsumerStatId().withTags(tags);
    }

    /**
     * Create provider id
     *
     * @param model RpcServerLookoutModel
     * @return Id
     */
    public Id createMethodProviderId(RpcServerLookoutModel model) {
        Map<String, String> tags = new HashMap<String, String>(5);

        tags.put("app", StringUtils.defaultString(model.getApp()));
        tags.put("service", StringUtils.defaultString(model.getService()));
        tags.put("method", StringUtils.defaultString(model.getMethod()));
        tags.put("protocol", StringUtils.defaultString(model.getProtocol()));
        tags.put("caller_app", StringUtils.defaultString(model.getCallerApp()));

        return rpcLookoutId.fetchProviderStatId().withTags(tags);
    }

    /**
     * Collect the RPC client information.
     *
     * @param providerConfig client information model
     */
    public void collectProvderPubInfo(final ProviderConfig providerConfig) {

        try {
            Id providerConfigId = rpcLookoutId.fetchProviderPubId();
            Lookout.registry().info(providerConfigId, new Info<ProviderConfig>() {
                @Override
                public ProviderConfig value() {
                    return providerConfig;
                }
            });
        } catch (Throwable t) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_METRIC_REPORT_ERROR), t);
        }
    }

    /**
     * Collect the RPC client information.
     *
     * @param consumerConfig client information model
     */
    public void collectConsumerSubInfo(final ConsumerConfig consumerConfig) {

        try {
            Id consumerConfigId = rpcLookoutId.fetchConsumerSubId();
            Lookout.registry().info(consumerConfigId, new Info<ConsumerConfig>() {
                @Override
                public ConsumerConfig value() {
                    return consumerConfig;
                }
            });
        } catch (Throwable t) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_METRIC_REPORT_ERROR), t);
        }
    }

    /**
     * Thread pool static configuration information.
     */
    private class ThreadPoolConfig {

        private int corePoolSize;

        private int maxPoolSize;

        private int queueSize;

        public ThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueSize) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
        }

        /**
         * Getter method for property <tt>corePoolSize</tt>.
         *
         * @return property value of corePoolSize
         */
        public int getCorePoolSize() {
            return corePoolSize;
        }

        /**
         * Setter method for property <tt>corePoolSize</tt>.
         *
         * @param corePoolSize value to be assigned to property corePoolSize
         */
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        /**
         * Getter method for property <tt>maxPoolSize</tt>.
         *
         * @return property value of maxPoolSize
         */
        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        /**
         * Setter method for property <tt>maxPoolSize</tt>.
         *
         * @param maxPoolSize value to be assigned to property maxPoolSize
         */
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        /**
         * Getter method for property <tt>queueSize</tt>.
         *
         * @return property value of queueSize
         */
        public int getQueueSize() {
            return queueSize;
        }

        /**
         * Setter method for property <tt>queueSize</tt>.
         *
         * @param queueSize value to be assigned to property queueSize
         */
        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }
    }
}