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
 * @version $Id: AuthRule.java, v 0.1 2019年04月11日 下午7:53 lepdou Exp $
 */
public class AuthRule {

    private String             name;
    private String             mode;
    private int                enabled;

    private List<AuthRuleItem> ruleItems;

    public boolean enable() {
        return enabled > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public List<AuthRuleItem> getRuleItems() {
        return ruleItems;
    }

    public void setRuleItems(List<AuthRuleItem> ruleItems) {
        this.ruleItems = ruleItems;
    }
}