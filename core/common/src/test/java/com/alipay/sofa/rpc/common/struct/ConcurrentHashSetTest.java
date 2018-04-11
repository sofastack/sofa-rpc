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
package com.alipay.sofa.rpc.common.struct;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ConcurrentHashSetTest {

    @Test
    public void testConstruct() {
        HashSet set = new HashSet();
        set.add("1");
        set.add("2");
        set.add("4");
        set.add("3");

        ConcurrentHashSet set1 = new ConcurrentHashSet(set);
        Assert.assertEquals(set1.size(), 4);

        List list = new ArrayList();
        list.add("1");
        list.add("2");
        list.add("3");

        ConcurrentHashSet set2 = new ConcurrentHashSet(list);
        Assert.assertEquals(set2.size(), 3);
    }
}