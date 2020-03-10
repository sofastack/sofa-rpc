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
package com.alipay.sofa.rpc.doc.swagger.resource;

import java.util.Date;
import java.util.List;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class ComplexPojo2 {

    private int                int0;
    private Integer            integer0;
    private byte               byte0;
    private byte[]             bytes0;
    private List<String>       stringList;
    private List<ComplexPojo2> complexPojoList;
    private String[]           strings;
    private ComplexPojo2[]     complexPojos;
    private Date               date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getInt0() {
        return int0;
    }

    public void setInt0(int int0) {
        this.int0 = int0;
    }

    public Integer getInteger0() {
        return integer0;
    }

    public void setInteger0(Integer integer0) {
        this.integer0 = integer0;
    }

    public byte getByte0() {
        return byte0;
    }

    public void setByte0(byte byte0) {
        this.byte0 = byte0;
    }

    public byte[] getBytes0() {
        return bytes0;
    }

    public void setBytes0(byte[] bytes0) {
        this.bytes0 = bytes0;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public List<ComplexPojo2> getComplexPojoList() {
        return complexPojoList;
    }

    public void setComplexPojoList(List<ComplexPojo2> complexPojoList) {
        this.complexPojoList = complexPojoList;
    }

    public String[] getStrings() {
        return strings;
    }

    public void setStrings(String[] strings) {
        this.strings = strings;
    }

    public ComplexPojo2[] getComplexPojos() {
        return complexPojos;
    }

    public void setComplexPojos(ComplexPojo2[] complexPojos) {
        this.complexPojos = complexPojos;
    }
}
