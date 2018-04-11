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
package com.alipay.sofa.rpc.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AllTypeServiceImpl implements AllTypeService {
    @Override
    public void echo() {

    }

    @Override
    public String echo2(String a, Integer b) {
        return a + "" + b;
    }

    @Override
    public int echoInt(int i) {
        return i;
    }

    @Override
    public AllTypeEnum echoEnum(AllTypeEnum enum1) {
        return enum1;
    }

    @Override
    public AllTypeEnum2 echoEnum2(AllTypeEnum2 enum2) {
        return enum2;
    }

    @Override
    public String[] echoStringArray(String[] strings) {
        return strings;
    }

    @Override
    public String[][] echoStringArray2(String[][] strings) {
        return strings;
    }

    @Override
    public List echoList(List list) {
        return list;
    }

    @Override
    public Set echoSet(Set set) {
        return set;
    }

    @Override
    public Map echoMap(Map map) {
        return map;
    }

    @Override
    public Date echoDate(Date date) {
        return date;
    }

    @Override
    public MyList echoMyList(MyList list) {
        return list;
    }

    @Override
    public MySet echoMySet(MySet set) {
        return set;
    }

    @Override
    public MyMap echoMyMap(MyMap map) {
        return map;
    }

    @Override
    public BigDecimal echoNum(BigDecimal bigDecimal, BigInteger bigInteger) {
        return bigDecimal.multiply(new BigDecimal(bigInteger.toString()));
    }

    @Override
    public BigInteger echoBigInteger(BigInteger bigInteger) {
        return bigInteger;
    }

    @Override
    public Currency echoCurrency(Currency currency) {
        return currency;
    }

    @Override
    public AllTypeObj echoObj(AllTypeObj obj) {
        return obj;
    }

    @Override
    public IAllType echoInterfaceObj(IAllType obj) {
        return obj;
    }

    @Override
    public AllTypeObj echoSubObj(AllTypeObj obj) {
        return obj;
    }
}
