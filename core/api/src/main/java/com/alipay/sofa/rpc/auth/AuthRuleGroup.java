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
package com.alipay.sofa.rpc.auth;

import java.util.List;

/**
 *
 * @author lepdou
 * @version $Id: AuthRuleGroup.java, v 0.1 2019年04月11日 下午7:53 lepdou Exp $
 */
public class AuthRuleGroup {

    private String         dataId;
    private String         type;
    private int            enabled;

    private List<AuthRule> rules;

    public boolean enable() {
        return enabled > 0;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public List<AuthRule> getRules() {
        return rules;
    }

    public void setRules(List<AuthRule> rules) {
        this.rules = rules;
    }
}