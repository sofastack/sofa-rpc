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
import org.mockito.internal.util.collections.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetDifferenceTest {

    @Test
    public void testDifference() throws Exception {
        Set<String> s1 = Sets.newSet("111", "222", "333");
        Set<String> s2 = Sets.newSet("111", "333", "444", "555", "666");

        SetDifference difference = new SetDifference(s1, s2);
        Assert.assertFalse(difference.areEqual());

        List<String> add = difference.getOnlyOnRight();
        List<String> removed = difference.getOnlyOnLeft();
        List<String> same = difference.getOnBoth();

        Assert.assertEquals(add.size(), 3);
        Assert.assertEquals(removed.size(), 1);
        Assert.assertEquals(same.size(), 2);

        Assert.assertEquals(add.get(0), "444");
        Assert.assertEquals(removed.get(0), "222");

        s1 = Sets.newSet("111", "222", "333");
        s2 = Sets.newSet("111", "333", "222");

        difference = new SetDifference(s1, s2);
        Assert.assertTrue(difference.areEqual());

        add = difference.getOnlyOnRight();
        removed = difference.getOnlyOnLeft();
        same = difference.getOnBoth();

        Assert.assertEquals(add.size(), 0);
        Assert.assertEquals(removed.size(), 0);
        Assert.assertEquals(same.size(), 3);

        s1 = new HashSet<>();
        s2 = Sets.newSet();
        difference = new SetDifference(s1, s2);
        Assert.assertTrue(difference.areEqual());

        s1 = null;
        s2 = Sets.newSet();
        difference = new SetDifference(s1, s2);
        Assert.assertTrue(difference.areEqual());

        s1 = Collections.emptySet();
        s2 = null;
        difference = new SetDifference(s1, s2);
        Assert.assertTrue(difference.areEqual());
    }
}
