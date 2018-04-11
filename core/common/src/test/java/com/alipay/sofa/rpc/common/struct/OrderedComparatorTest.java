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

import com.alipay.sofa.rpc.base.Sortable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class OrderedComparatorTest {

    @Test
    public void testCompare() throws Exception {
        List<Obj> list = new ArrayList<Obj>();
        list.add(new Obj("a", 10));
        list.add(new Obj("b", 2));
        list.add(new Obj("c", 6));
        list.add(new Obj("d", 6));
        list.add(new Obj("e", 0));
        list.add(new Obj("f", -1));
        list.add(new Obj("g", 10));

        Collections.sort(list, new OrderedComparator<Obj>());
        StringBuilder sb = new StringBuilder();
        for (Obj test : list) {
            sb.append(test.getName());
        }
        Assert.assertEquals(sb.toString(), "febcdag");

        Collections.sort(list, new OrderedComparator<Obj>(false));
        sb = new StringBuilder();
        for (Obj test : list) {
            sb.append(test.getName());
        }
        Assert.assertEquals(sb.toString(), "agcdbef");
    }

    private static class Obj implements Sortable {
        private final String name;
        private final int    order;

        Obj(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }

        public String getName() {
            return name;
        }
    }

}