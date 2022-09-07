/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.sofa.rpc.common.struct;

import com.alipay.sofa.rpc.common.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * 比较两个set的不同，列出差异部分：包括左侧独有，右侧独有，双方都有
 * @author xiaojian.xj
 * @version : SetDifference.java, v 0.1 2022年09月07日 15:39 xiaojian.xj Exp $
 */
public class SetDifference<T> {

    /**
     * The Only on left.
     */
    private List<T> onlyOnLeft;

    /**
     * The Only on right.
     */
    private List<T> onlyOnRight;

    /**
     * The On both.
     */
    private List<T> onBoth;

    public SetDifference(Set<? extends T> left, Set<? extends T> right) {
        if (CommonUtils.isEmpty(left) || CommonUtils.isEmpty(right)) {
            this.onlyOnLeft = Collections.unmodifiableList(left == null ? new ArrayList<T>() : new ArrayList<T>(left));
            this.onlyOnRight = Collections.unmodifiableList(right == null ? new ArrayList<T>() : new ArrayList<T>(right));
            this.onBoth = Collections.unmodifiableList(new ArrayList<T>());
            return;
        }

        List<T> onlyOnLeft = new ArrayList<>(left.size());
        List<T> onlyOnRight = new ArrayList<T>(right.size());
        List<T> onBoth = new ArrayList<T>(left.size());

        for (T leftValue : left) {
            if (right.contains(leftValue)) {
                onBoth.add(leftValue);
            } else {
                onlyOnLeft.add(leftValue);
            }
        }

        for (T rightValue : right) {
            if (!left.contains(rightValue)) {
                onlyOnRight.add(rightValue);
            }
        }

        this.onlyOnLeft = Collections.unmodifiableList(onlyOnLeft);
        this.onlyOnRight = Collections.unmodifiableList(onlyOnRight);
        this.onBoth = Collections.unmodifiableList(onBoth);
    }

    /**
     * Are equal.
     *
     * @return the boolean
     */
    public boolean areEqual() {
        return onlyOnLeft.isEmpty() && onlyOnRight.isEmpty();
    }

    /**
     * Gets only on left.
     *
     * @return the only on left
     */
    public List<T> getOnlyOnLeft() {
        return onlyOnLeft;
    }

    /**
     * Gets only on right.
     *
     * @return the only on right
     */
    public List<T> getOnlyOnRight() {
        return onlyOnRight;
    }

    /**
     * Gets on both.
     *
     * @return the on both
     */
    public List<T> getOnBoth() {
        return onBoth;
    }
}
