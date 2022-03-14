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

import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * 调控模型
 * 对指定应用和服务下的一系列Invocation的调控。
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class MeasureModel {

    /**
     * App name of measure model
     */
    private final String                            appName;
    /**
     * service name of measure model
     */
    private final String                            service;
    /**
     * all dimension statics stats of measure model
     */
    private final ConcurrentHashSet<InvocationStat> stats = new ConcurrentHashSet<InvocationStat>();

    /**
     * Instantiates a new Measure model.
     *
     * @param appName the app name
     * @param service the service
     */
    public MeasureModel(String appName, String service) {
        this.appName = appName;
        this.service = service;
    }

    /**
     * Add dimension boolean.
     *
     * @param statDimension the dimension
     * @return the boolean
     */
    public boolean addInvocationStat(InvocationStat statDimension) {
        return stats.add(statDimension);
    }

    /**
     * Remove dimension.
     *
     * @param statDimension the dimension
     */
    public void removeInvocationStat(InvocationStat statDimension) {
        stats.remove(statDimension);
    }

    /**
     * Is empty boolean.
     *
     * @return the boolean
     */
    public boolean isEmpty() {
        return stats.isEmpty();
    }

    /**
     * Getter method for property <tt>stats</tt>.
     *
     * @return property value of stats
     */
    public List<InvocationStat> getInvocationStats() {
        return new ArrayList<InvocationStat>(stats);
    }

    /**
     * Getter method for property <tt>appName</tt>.
     *
     * @return property value of appName
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Getter method for property <tt>service</tt>.
     *
     * @return property value of service
     */
    public String getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MeasureModel that = (MeasureModel) o;

        if (!appName.equals(that.appName)) {
            return false;
        }
        return service.equals(that.service);
    }

    @Override
    public int hashCode() {
        int result = appName.hashCode();
        result = 31 * result + service.hashCode();
        return result;
    }

}