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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AllTypeServiceTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setDaemon(false);

        ProviderConfig<AllTypeService> CProvider = new ProviderConfig<AllTypeService>()
            .setInterfaceId(AllTypeService.class.getName())
            .setRef(new AllTypeServiceImpl())
            .setServer(serverConfig2);
        CProvider.export();

        ConsumerConfig<AllTypeService> BConsumer = new ConsumerConfig<AllTypeService>()
            .setInterfaceId(AllTypeService.class.getName())
            .setTimeout(50000)
            .setSerialization("hessian")
            .setDirectUrl("bolt://127.0.0.1:22222");
        AllTypeService helloService = BConsumer.refer();

        // 基本类型和数组
        helloService.echo();
        Assert.assertEquals("a1", helloService.echo2("a", 1));
        Assert.assertEquals(1, helloService.echoInt(1));

        Assert.assertEquals(AllTypeEnum.TB, helloService.echoEnum(AllTypeEnum.TB));
        Assert.assertEquals(AllTypeEnum2.TB, helloService.echoEnum2(AllTypeEnum2.TB));

        Assert
            .assertArrayEquals(new String[] { "11", "22" }, helloService.echoStringArray(new String[] { "11", "22" }));
        Assert.assertArrayEquals(new String[][] { { "aa", "bb" }, { "11", "22" } },
            helloService.echoStringArray2(new String[][] { { "aa", "bb" }, { "11", "22" } }));

        // 集合
        List<String> list = new ArrayList<String>();
        list.add("11");
        list.add("22");
        Assert.assertEquals(list, helloService.echoList(list));
        list = new LinkedList<String>();
        list.add("11");
        list.add("22");
        Assert.assertEquals(list, helloService.echoList(list));
        list = new CopyOnWriteArrayList<String>();
        list.add("11");
        list.add("22");
        Assert.assertEquals(list, helloService.echoList(list));
        list = new LinkedList<String>();
        list.add("11");
        list.add("22");
        Assert.assertEquals(list, helloService.echoList(list));
        list = Arrays.asList("11", "22");
        Assert.assertEquals(list, helloService.echoList(list));

        MyList<String> myList = new MyList<String>();
        myList.setListName("xxx");
        myList.add("11");
        myList.add("22");
        MyList myList1 = helloService.echoMyList(myList);
        // Assert.assertEquals(myList.getListName(), myList1.getListName()); // TODO 目前不支持自定义LIST的字段
        // Assert.assertEquals(myList, myList1);
        Assert.assertEquals(myList.get(0), myList1.get(0));

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("xx", 11);
        map.put("yy", 22);
        Assert.assertEquals(map, helloService.echoMap(map));
        map = new LinkedHashMap<String, Integer>();
        map.put("xx", 11);
        map.put("yy", 22);
        Assert.assertEquals(map, helloService.echoMap(map));
        map = new TreeMap<String, Integer>();
        map.put("xx", 11);
        map.put("yy", 22);
        Assert.assertEquals(map, helloService.echoMap(map));
        map = new ConcurrentHashMap<String, Integer>();
        map.put("xx", 11);
        map.put("yy", 22);
        Assert.assertEquals(map, helloService.echoMap(map));
        map = Collections.singletonMap("xx", 11);
        Assert.assertEquals(map, helloService.echoMap(map));
        MyMap myMap = new MyMap();
        myMap.setMapName("map");
        myMap.put("xx", 11);
        myMap.put("yy", 22);
        MyMap myMap1 = helloService.echoMyMap(myMap);
        // Assert.assertEquals(myMap.getMapName(), myMap1.getMapName()); // TODO 目前不支持自定义LIST的字段
        // Assert.assertEquals(myMap, myMap1);
        Assert.assertEquals(myMap.get("xx"), myMap1.get("xx"));

        Set<String> set = new HashSet<String>();
        set.add("11");
        set.add("22");
        Assert.assertEquals(set, helloService.echoSet(set));
        set = new TreeSet<String>();
        set.add("11");
        set.add("22");
        Assert.assertEquals(set, helloService.echoSet(set));
        set = new LinkedHashSet<String>();
        set.add("11");
        set.add("22");
        Assert.assertEquals(set, helloService.echoSet(set));
        set = new ConcurrentHashSet<String>();
        set.add("11");
        set.add("22");
        Assert.assertEquals(set, helloService.echoSet(set));
        MySet<String> mySet = new MySet<String>();
        mySet.setSetName("set");
        myList.add("11");
        myList.add("22");
        MySet mySet1 = helloService.echoMySet(mySet);
        // Assert.assertEquals(mySet.getSetName(), mySet1.getSetName()); // TODO 目前不支持自定义LIST的字段
        // Assert.assertEquals(mySet, mySet1);
        Assert.assertEquals(mySet.size(), mySet1.size());

        // 常用类型
        Date date = new Date();
        Assert.assertEquals(date, helloService.echoDate(date));

        Assert.assertEquals(new BigDecimal("6.6"), helloService.echoNum(new BigDecimal("2.2"), new BigInteger("3")));
        Assert.assertEquals(new BigInteger("3"), helloService.echoBigInteger(new BigInteger("3")));

        Assert.assertEquals(Currency.getInstance("CNY"), helloService.echoCurrency(Currency.getInstance("CNY")));

        // 自定义类型
        AllTypeObj obj = new AllTypeObj();
        AllTypeSubObj subObj = new AllTypeSubObj();
        Assert.assertEquals(obj, helloService.echoObj(obj));
        Assert.assertEquals(subObj, helloService.echoObj(subObj));

        Assert.assertEquals(obj, helloService.echoInterfaceObj(obj));
        Assert.assertEquals(subObj, helloService.echoInterfaceObj(subObj));

        Assert.assertEquals(obj, helloService.echoSubObj(obj));
        Assert.assertEquals(subObj, helloService.echoSubObj(subObj));

        {
            ConsumerConfig<AllTypeService> aConsumer = new ConsumerConfig<AllTypeService>()
                .setInterfaceId(AllTypeService.class.getName())
                .setTimeout(50000)
                .setSerialization("hessian")
                .setRepeatedReferLimit(-1)
                .setProxy("javassist")
                .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
                .setDirectUrl("bolt://127.0.0.1:22222");
            AllTypeService helloService2 = aConsumer.refer();
            Assert.assertEquals(0, helloService2.echoInt(1));
            boolean error = false;
            Integer v = null;
            try {
                v = (Integer) RpcInvokeContext.getContext().getFuture().get();
            } catch (Exception e) {
                error = true;
            }
            Assert.assertFalse(error);
            Assert.assertTrue(v == 1);
        }

        {
            ConsumerConfig<AllTypeService> aConsumer = new ConsumerConfig<AllTypeService>()
                .setInterfaceId(AllTypeService.class.getName())
                .setTimeout(50000)
                .setSerialization("hessian")
                .setRepeatedReferLimit(-1)
                .setProxy("jdk")
                .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
                .setDirectUrl("bolt://127.0.0.1:22222");
            AllTypeService helloService2 = aConsumer.refer();
            Assert.assertEquals(0, helloService2.echoInt(1));
            boolean error = false;
            Integer v = null;
            try {
                v = (Integer) RpcInvokeContext.getContext().getFuture().get();
            } catch (Exception e) {
                error = true;
            }
            Assert.assertFalse(error);
            Assert.assertTrue(v == 1);
        }
    }
}
