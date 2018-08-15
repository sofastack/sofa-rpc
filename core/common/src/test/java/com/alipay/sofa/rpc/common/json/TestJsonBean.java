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
package com.alipay.sofa.rpc.common.json;

import java.util.ArrayList;
import java.util.Set;

/**
 *
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class TestJsonBean {

    @JSONField(alias = "Name", isRequired = true)
    private String                  name;
    @JSONField(alias = "Sex")
    private boolean                 sex;
    private int                     age;
    @JSONField(skipIfNull = true)
    private ArrayList<TestJsonBean> friends;
    @JSONField(skipIfNull = true)
    private Set<TestJsonBean>       vips;
    @JSONField(alias = "Remark")
    private Object[]                remark;

    @JSONField(skipIfNull = true)
    private Status                  status;

    private Long                    step;

    private static String           staticString;
    private transient String        transString;
    @JSONIgnore
    private String                  ignoreString;

    public String getName() {
        return name;
    }

    public boolean isSex() {
        return sex;
    }

    public int getAge() {
        return age;
    }

    public ArrayList<TestJsonBean> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<TestJsonBean> friends) {
        this.friends = friends;
    }

    public Set<TestJsonBean> getVips() {
        return vips;
    }

    public TestJsonBean setVips(Set<TestJsonBean> vips) {
        this.vips = vips;
        return this;
    }

    public Object[] getRemark() {
        return remark;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setRemark(Object[] remark) {
        this.remark = remark;
    }

    public Long getStep() {
        return step;
    }

    public void setStep(Long step) {
        this.step = step;
    }

    public String getTransString() {
        return transString;
    }

    public void setTransString(String transString) {
        this.transString = transString;
    }

    public String getIgnoreString() {
        return ignoreString;
    }

    public void setIgnoreString(String ignoreString) {
        this.ignoreString = ignoreString;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        START(0, "启动"),
        STOP(1, "停止");

        int    code;
        String name;

        Status(int code, String name) {
            this.code = code;
            this.name = name();
        }
    }
}
