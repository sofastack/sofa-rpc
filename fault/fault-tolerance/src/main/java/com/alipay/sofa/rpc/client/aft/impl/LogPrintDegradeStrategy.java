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
import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.client.aft.MeasureResultDetail;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * 日志打印降级策略 对异常ip进行异常信息的日志打印
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Extension("log")
public class LogPrintDegradeStrategy implements DegradeStrategy {

    /** Logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LogPrintDegradeStrategy.class);

    @Override
    public void degrade(MeasureResultDetail measureResultDetail) {
        InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
        String appName = statDimension.getAppName();
        if (LOGGER.isInfoEnabled(appName)) {
            String service = statDimension.getService();
            long timeWindow = measureResultDetail.getTimeWindow();
            long windowCount = measureResultDetail.getWindowCount();
            double abnormalRate = measureResultDetail.getAbnormalRate();
            double averageAbnormalRate = measureResultDetail.getAverageAbnormalRate();
            String ip = statDimension.getIp();

            LOGGER.infoWithApp(appName,
                LogCodes.getLog(LogCodes.INFO_REGULATION_ABNORMAL, timeWindow, service, appName,
                    windowCount, abnormalRate, averageAbnormalRate, ip));
        }
    }
}