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
package com.alipay.sofa.rpc.common.utils;

import java.util.List;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TestOtherBean {

    private String    alias;
    private int       heartbeat;
    private boolean   register;
    List<TestSubBean> subBeans;

    public String getAlias() {
        return alias;
    }

    public TestOtherBean setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public int getHeartbeat() {
        return heartbeat;
    }

    public TestOtherBean setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
        return this;
    }

    public boolean isRegister() {
        return register;
    }

    public TestOtherBean setRegister(boolean register) {
        this.register = register;
        return this;
    }

    public List<TestSubBean> getSubBeans() {
        return subBeans;
    }

    public TestOtherBean setSubBeans(List<TestSubBean> subBeans) {
        this.subBeans = subBeans;
        return this;
    }
}
