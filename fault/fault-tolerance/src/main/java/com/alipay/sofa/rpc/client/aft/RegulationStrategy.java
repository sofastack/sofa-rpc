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
package com.alipay.sofa.rpc.client.aft;

import com.alipay.sofa.rpc.ext.Extensible;

/**
 * 能力调控策略
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Extensible(singleton = false)
public interface RegulationStrategy {

    /**
     * Is degrade effective
     *
     * @param measureResultDetail MeasureResultDetail
     * @return is degrade enable
     */
    boolean isDegradeEffective(MeasureResultDetail measureResultDetail);

    /**
     * Is reach max degrade ip limit. just degrade specified num ip.
     *
     * @param measureResultDetail MeasureResultDetail
     * @return is reach max count
     */
    boolean isReachMaxDegradeIpCount(MeasureResultDetail measureResultDetail);

    /**
     * Is the ip already exist in the degrade ip list
     *
     * @param measureResultDetail MeasureResultDetail
     * @return is exists
     */
    boolean isExistInTheDegradeList(MeasureResultDetail measureResultDetail);

    /**
     * remove ip from degrade ip list
     *
     * @param measureResultDetail MeasureResultDetail
     */
    void removeFromDegradeList(MeasureResultDetail measureResultDetail);
}