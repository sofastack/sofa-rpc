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
package com.alipay.sofa.rpc.model;

/**
 * The model for lookout info of server
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liengen</a>
 * @version $Id: RpcServerMetricsModel.java, v 0.1 2018年04月24日 下午4:22 LiWei.Liengen Exp $
 */
public class RpcServerLookoutModel extends RpcAbstractLookoutModel {

    protected String callerApp;

    /**
     * Getter method for property <tt>callerApp</tt>.
     *
     * @return property value of callerApp
     */
    public String getCallerApp() {
        return callerApp;
    }

    /**
     * Setter method for property <tt>callerApp</tt>.
     *
     * @param callerApp  value to be assigned to property callerApp
     */
    public void setCallerApp(String callerApp) {
        this.callerApp = callerApp;
    }
}