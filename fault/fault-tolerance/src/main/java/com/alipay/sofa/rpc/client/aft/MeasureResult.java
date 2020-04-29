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

import java.util.ArrayList;
import java.util.List;

/**
 * 资源度量结果
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class MeasureResult {

    /**
     * model for measure
     */
    private MeasureModel                    measureModel;

    /**
     * the detail result of measure
     */
    private final List<MeasureResultDetail> measureResultDetails = new ArrayList<MeasureResultDetail>();

    /**
     * Add measure detail.
     *
     * @param measureResultDetail the measure detail
     */
    public void addMeasureDetail(MeasureResultDetail measureResultDetail) {
        measureResultDetails.add(measureResultDetail);
    }

    /**
     * Gets all measure details.
     *
     * @return the all measure details
     */
    public List<MeasureResultDetail> getAllMeasureResultDetails() {
        return measureResultDetails;
    }

    /**
     * Gets measure model.
     *
     * @return the measure model
     */
    public MeasureModel getMeasureModel() {
        return measureModel;
    }

    /**
     * Sets measure model.
     *
     * @param measureModel the measure model
     * @return the measure model
     */
    public MeasureResult setMeasureModel(MeasureModel measureModel) {
        this.measureModel = measureModel;
        return this;
    }
}