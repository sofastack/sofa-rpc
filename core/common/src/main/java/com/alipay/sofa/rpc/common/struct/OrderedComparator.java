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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Default comparator of sortable.
 *
 * @param <T> the type parameter
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class OrderedComparator<T extends Sortable> implements Comparator<T>, Serializable {

    /**
     * 顺序（true:从小到大）还是倒序（false:从大到小）
     */
    private final boolean order;

    /**
     * 默认顺序，从小到大
     */
    public OrderedComparator() {
        this.order = true;
    }

    /**
     * Instantiates a new Ordered comparator.
     *
     * @param smallToLarge the small to large
     */
    public OrderedComparator(boolean smallToLarge) {
        this.order = smallToLarge;
    }

    @Override
    public int compare(T o1, T o2) {
        // order一样的情况下，顺序不变
        return order ? o1.getOrder() - o2.getOrder() :
            o2.getOrder() - o1.getOrder();
    }
}
