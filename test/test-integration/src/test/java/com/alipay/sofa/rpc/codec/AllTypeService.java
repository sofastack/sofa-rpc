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
public interface AllTypeService {

    // 基本类型
    public void echo();

    public String echo2(String a, Integer b);

    public int echoInt(int i);

    public AllTypeEnum echoEnum(AllTypeEnum enum1);

    public AllTypeEnum2 echoEnum2(AllTypeEnum2 enum2);

    // 数组
    public String[] echoStringArray(String[] strings);

    public String[][] echoStringArray2(String[][] strings);

    // 集合
    public List echoList(List list);

    public Set echoSet(Set set);

    public Map echoMap(Map map);

    public Date echoDate(Date date);

    public MyList echoMyList(MyList list);

    public MySet echoMySet(MySet set);

    public MyMap echoMyMap(MyMap map);

    // 常用类型 
    public BigDecimal echoNum(BigDecimal bigDecimal, BigInteger bigInteger);

    public BigInteger echoBigInteger(BigInteger bigInteger);

    public Currency echoCurrency(Currency currency);

    // 自定义对象
    public AllTypeObj echoObj(AllTypeObj obj);

    public IAllType echoInterfaceObj(IAllType obj);

    public AllTypeObj echoSubObj(AllTypeObj obj);

}
