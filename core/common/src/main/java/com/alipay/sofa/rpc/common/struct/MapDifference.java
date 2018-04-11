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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Map比较结果，比较两个Map，得到两个map的差别，返回左边独有，右边独有，两个都有且相同，两个都有但不同（不同值）<br>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @see ValueDifference
 */
public class MapDifference<K, V> {

    /**
     * The Only on left.
     */
    private Map<K, V>                  onlyOnLeft;
    /**
     * The Only on right.
     */
    private Map<K, V>                  onlyOnRight;
    /**
     * The On both.
     */
    private Map<K, V>                  onBoth;
    /**
     * The Differences.
     */
    private Map<K, ValueDifference<V>> differences;

    /**
     * Instantiates a new Map difference.
     *
     * @param left
     *         the left
     * @param right
     *         the right
     */
    public MapDifference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right) {
        boolean switched = false;
        if (left.size() < right.size()) { // 做优化，比较大小，只遍历少的
            Map<? extends K, ? extends V> tmp = left;
            left = right;
            right = tmp;
            switched = true;
        }

        Map<K, V> onlyOnLeft = new HashMap<K, V>();
        Map<K, V> onlyOnRight = new HashMap<K, V>(right);
        Map<K, V> onBoth = new HashMap<K, V>();
        Map<K, ValueDifference<V>> differences = new HashMap<K, ValueDifference<V>>();

        for (Map.Entry<? extends K, ? extends V> entry : left.entrySet()) {
            K leftKey = entry.getKey();
            V leftValue = entry.getValue();
            if (right.containsKey(leftKey)) {
                final V rightValue = onlyOnRight.remove(leftKey);
                if (valueEquals(leftValue, rightValue)) {
                    onBoth.put(leftKey, leftValue);
                } else {
                    differences.put(leftKey,
                        switched ? new ValueDifference<V>(rightValue, leftValue) :
                            new ValueDifference<V>(leftValue, rightValue));
                }
            } else {
                onlyOnLeft.put(leftKey, leftValue);
            }
        }
        this.onlyOnLeft = Collections.unmodifiableMap(switched ? onlyOnRight : onlyOnLeft);
        this.onlyOnRight = Collections.unmodifiableMap(switched ? onlyOnLeft : onlyOnRight);
        this.onBoth = Collections.unmodifiableMap(onBoth);
        this.differences = Collections.unmodifiableMap(differences);
    }

    /**
     * Value equals.
     *
     * @param leftValue
     *         the left value
     * @param rightValue
     *         the right value
     * @return the boolean
     */
    private boolean valueEquals(V leftValue, V rightValue) {
        if (leftValue == rightValue) {
            return true;
        }
        if (leftValue == null || rightValue == null) {
            return false;
        }
        return leftValue.equals(rightValue);
    }

    /**
     * Are equal.
     *
     * @return the boolean
     */
    public boolean areEqual() {
        return onlyOnLeft.isEmpty() && onlyOnRight.isEmpty() && differences.isEmpty();
    }

    /**
     * Entries only on left.
     *
     * @return the map
     */
    public Map<K, V> entriesOnlyOnLeft() {
        return onlyOnLeft;
    }

    /**
     * Entries only on right.
     *
     * @return the map
     */
    public Map<K, V> entriesOnlyOnRight() {
        return onlyOnRight;
    }

    /**
     * Entries in common.
     *
     * @return the map
     */
    public Map<K, V> entriesInCommon() {
        return onBoth;
    }

    /**
     * Entries differing.
     *
     * @return the map
     */
    public Map<K, ValueDifference<V>> entriesDiffering() {
        return differences;
    }

}
