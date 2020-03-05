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
package com.alipay.sofa.rpc.client.aft.impl;

import com.alipay.sofa.rpc.client.aft.DegradeStrategy;
import com.alipay.sofa.rpc.client.aft.FaultToleranceConfigManager;
import com.alipay.sofa.rpc.client.aft.InvocationStat;
import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.client.aft.InvocationStatFactory;
import com.alipay.sofa.rpc.client.aft.InvocationStatFactory.InvocationStatListener;
import com.alipay.sofa.rpc.client.aft.MeasureModel;
import com.alipay.sofa.rpc.client.aft.MeasureResult;
import com.alipay.sofa.rpc.client.aft.MeasureResultDetail;
import com.alipay.sofa.rpc.client.aft.MeasureState;
import com.alipay.sofa.rpc.client.aft.MeasureStrategy;
import com.alipay.sofa.rpc.client.aft.RecoverStrategy;
import com.alipay.sofa.rpc.client.aft.RegulationStrategy;
import com.alipay.sofa.rpc.client.aft.Regulator;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.struct.ScheduledService;
import com.alipay.sofa.rpc.common.utils.ThreadPoolUtils;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按时间窗口进行调控
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("timeWindow")
public class TimeWindowRegulator implements Regulator {

    /** Logger for this class */
    private static final Logger                      LOGGER             = LoggerFactory
                                                                            .getLogger(TimeWindowRegulator.class);

    /** Counter for measure schedule and calculate time window */
    private final AtomicInteger                      measureCounter     = new AtomicInteger();

    /**
     * 度量线程池
     */
    private final ScheduledService                   measureScheduler   = new ScheduledService("AFT-MEASURE",
                                                                            ScheduledService.MODE_FIXEDRATE,
                                                                            new MeasureRunnable(), 1, 1,
                                                                            TimeUnit.SECONDS);
    /**
     * Is measure scheduler started
     */
    private final AtomicBoolean                      measureStarted     = new AtomicBoolean();

    /**
     * 计算线程池
     */
    private final ExecutorService                    regulationExecutor = ThreadPoolUtils.newFixedThreadPool(2,
                                                                            new LinkedBlockingQueue<Runnable>(16),
                                                                            new NamedThreadFactory(
                                                                                "AFT-REGULATION"));

    /**
     * 度量模型
     */
    private final CopyOnWriteArrayList<MeasureModel> measureModels      = new CopyOnWriteArrayList<MeasureModel>();

    /**
     * 度量策略（创建计算模型, 对计算模型里的数据进行度量，选出正常和异常节点）
     */
    private MeasureStrategy                          measureStrategy;

    /**
     * 计算策略（根据度量结果，判断是否需要执行降级或者恢复） 
     */
    private RegulationStrategy                       regulationStrategy;

    /**
     * 降级策略: 例如调整权重 
     */
    private DegradeStrategy                          degradeStrategy;

    /**
     * 恢复策略：例如调整权重 
     */
    private RecoverStrategy                          recoverStrategy;

    /**
     * Listener for invocation stat change.
     */
    private final InvocationStatListener             listener           = new TimeWindowRegulatorListener();

    @Override
    public void init() {
        String measureStrategyAlias = RpcConfigs
            .getOrDefaultValue(RpcOptions.AFT_MEASURE_STRATEGY, "serviceHorizontal");
        String regulationStrategyAlias = RpcConfigs.getOrDefaultValue(RpcOptions.AFT_REGULATION_STRATEGY,
            "serviceHorizontal");
        String degradeStrategyAlias = RpcConfigs.getOrDefaultValue(RpcOptions.AFT_DEGRADE_STRATEGY, "weight");
        String recoverStrategyAlias = RpcConfigs.getOrDefaultValue(RpcOptions.AFT_RECOVER_STRATEGY, "weight");

        measureStrategy = ExtensionLoaderFactory.getExtensionLoader(MeasureStrategy.class).getExtension(
            measureStrategyAlias);
        regulationStrategy = ExtensionLoaderFactory.getExtensionLoader(RegulationStrategy.class).getExtension(
            regulationStrategyAlias);
        degradeStrategy = ExtensionLoaderFactory.getExtensionLoader(DegradeStrategy.class).getExtension(
            degradeStrategyAlias);
        recoverStrategy = ExtensionLoaderFactory.getExtensionLoader(RecoverStrategy.class).getExtension(
            recoverStrategyAlias);

        InvocationStatFactory.addListener(listener);
    }

    @Override
    public void startRegulate() {
        if (measureStarted.compareAndSet(false, true)) {
            measureScheduler.start();
        }
    }

    @Override
    public void stopRegulate() {
        if (measureStarted.compareAndSet(true, false)) {
            measureScheduler.stop();
        }
    }

    @Override
    public void destroy() {
        // close scheduled service
        stopRegulate();

        // remove cache resource
        InvocationStatFactory.removeListener(listener);
        measureModels.clear();
        measureCounter.set(0);

        // release strategy
        measureStrategy = null;
        regulationStrategy = null;
        degradeStrategy = null;
        recoverStrategy = null;
    }

    /**
     * The thread which do measure
     */
    private class MeasureRunnable implements Runnable {

        @Override
        public void run() {
            measureCounter.incrementAndGet();
            for (MeasureModel measureModel : measureModels) {
                try {
                    if (isArriveTimeWindow(measureModel)) {
                        MeasureResult measureResult = measureStrategy.measure(measureModel);
                        regulationExecutor.submit(new RegulationRunnable(measureResult));
                    }
                } catch (Exception e) {
                    LOGGER.errorWithApp(measureModel.getAppName(),
                        LogCodes.getLog(LogCodes.ERROR_WHEN_DO_MEASURE, e.getMessage()), e);
                }
            }
        }

        private boolean isArriveTimeWindow(MeasureModel measureModel) {
            long timeWindow = FaultToleranceConfigManager.getTimeWindow(measureModel.getAppName());
            return measureCounter.get() % timeWindow == 0;
        }
    }

    /**
     * The thread which do regulation
     */
    private class RegulationRunnable implements Runnable {

        private final MeasureResult measureResult;

        /**
         * Instantiates a new Regulation runnable.
         *
         * @param measureResult the measure result
         */
        RegulationRunnable(MeasureResult measureResult) {
            this.measureResult = measureResult;
        }

        @Override
        public void run() {
            List<MeasureResultDetail> measureResultDetails = measureResult.getAllMeasureResultDetails();
            for (MeasureResultDetail measureResultDetail : measureResultDetails) {
                try {
                    doRegulate(measureResultDetail);
                } catch (Exception e) {
                    LOGGER.errorWithApp(measureResult.getMeasureModel().getAppName(),
                        LogCodes.getLog(LogCodes.ERROR_WHEN_DO_REGULATE, e.getMessage()), e);
                }
            }
        }

        /**
         * Do regulate.
         *
         * @param measureResultDetail the measure result detail
         */
        void doRegulate(MeasureResultDetail measureResultDetail) {
            MeasureState measureState = measureResultDetail.getMeasureState();
            InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();

            boolean isDegradeEffective = regulationStrategy.isDegradeEffective(measureResultDetail);
            if (isDegradeEffective) {
                measureResultDetail.setLogOnly(false);
                if (measureState.equals(MeasureState.ABNORMAL)) {
                    boolean isReachMaxDegradeIpCount = regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail);
                    if (!isReachMaxDegradeIpCount) {
                        degradeStrategy.degrade(measureResultDetail);
                    } else {
                        String appName = measureResult.getMeasureModel().getAppName();
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGULATION_ABNORMAL_NOT_DEGRADE,
                                "Reach degrade number limit.", statDimension.getService(), statDimension.getIp(),
                                statDimension.getAppName()));
                        }
                    }
                } else if (measureState.equals(MeasureState.HEALTH)) {
                    boolean isExistDegradeList = regulationStrategy.isExistInTheDegradeList(measureResultDetail);
                    if (isExistDegradeList) {
                        recoverStrategy.recover(measureResultDetail);
                        regulationStrategy.removeFromDegradeList(measureResultDetail);
                    }
                    //没有被降级过，因此不需要被恢复。
                }
            } else {
                measureResultDetail.setLogOnly(true);
                if (measureState.equals(MeasureState.ABNORMAL)) {
                    degradeStrategy.degrade(measureResultDetail);
                    String appName = measureResult.getMeasureModel().getAppName();
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGULATION_ABNORMAL_NOT_DEGRADE,
                            "Degrade switch is off", statDimension.getService(),
                            statDimension.getIp(), statDimension.getAppName()));
                    }
                }
            }
        }
    }

    class TimeWindowRegulatorListener implements InvocationStatListener {
        @Override
        public void onAddInvocationStat(InvocationStat invocationStat) {
            if (measureStrategy != null) {
                MeasureModel measureModel = measureStrategy.buildMeasureModel(invocationStat);
                if (measureModel != null) {
                    measureModels.add(measureModel);
                    startRegulate();
                }
            }
        }

        @Override
        public void onRemoveInvocationStat(InvocationStat invocationStat) {
            if (measureStrategy != null) {
                measureStrategy.removeMeasureModel(invocationStat);
            }
        }
    }
}