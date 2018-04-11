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
public class AllTypeObj implements IAllType {

    private int        i;
    private byte       b;
    private short      s;
    private long       l;
    private boolean    z;
    private char       c;
    private double     d;
    private float      f;

    private Integer    i1;
    private Byte       b1;
    private Short      s1;
    private Long       L1;
    private Character  c1;
    private Double     d1;
    private float      f1;

    private String     str;

    private List       list;
    private Set        set;
    private Map        map;

    private BigDecimal bigDecimal;
    private Date       date;
    private Currency   currency;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AllTypeObj obj = (AllTypeObj) o;

        if (i != obj.i)
            return false;
        if (b != obj.b)
            return false;
        if (s != obj.s)
            return false;
        if (l != obj.l)
            return false;
        if (z != obj.z)
            return false;
        if (c != obj.c)
            return false;
        if (Double.compare(obj.d, d) != 0)
            return false;
        if (Float.compare(obj.f, f) != 0)
            return false;
        if (Float.compare(obj.f1, f1) != 0)
            return false;
        if (i1 != null ? !i1.equals(obj.i1) : obj.i1 != null)
            return false;
        if (b1 != null ? !b1.equals(obj.b1) : obj.b1 != null)
            return false;
        if (s1 != null ? !s1.equals(obj.s1) : obj.s1 != null)
            return false;
        if (L1 != null ? !L1.equals(obj.L1) : obj.L1 != null)
            return false;
        if (c1 != null ? !c1.equals(obj.c1) : obj.c1 != null)
            return false;
        if (d1 != null ? !d1.equals(obj.d1) : obj.d1 != null)
            return false;
        if (str != null ? !str.equals(obj.str) : obj.str != null)
            return false;
        if (list != null ? !list.equals(obj.list) : obj.list != null)
            return false;
        if (set != null ? !set.equals(obj.set) : obj.set != null)
            return false;
        if (map != null ? !map.equals(obj.map) : obj.map != null)
            return false;
        if (bigDecimal != null ? !bigDecimal.equals(obj.bigDecimal) : obj.bigDecimal != null)
            return false;
        if (date != null ? !date.equals(obj.date) : obj.date != null)
            return false;
        return currency != null ? currency.equals(obj.currency) : obj.currency == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = i;
        result = 31 * result + (int) b;
        result = 31 * result + (int) s;
        result = 31 * result + (int) (l ^ (l >>> 32));
        result = 31 * result + (z ? 1 : 0);
        result = 31 * result + (int) c;
        temp = Double.doubleToLongBits(d);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (f != +0.0f ? Float.floatToIntBits(f) : 0);
        result = 31 * result + (i1 != null ? i1.hashCode() : 0);
        result = 31 * result + (b1 != null ? b1.hashCode() : 0);
        result = 31 * result + (s1 != null ? s1.hashCode() : 0);
        result = 31 * result + (L1 != null ? L1.hashCode() : 0);
        result = 31 * result + (c1 != null ? c1.hashCode() : 0);
        result = 31 * result + (d1 != null ? d1.hashCode() : 0);
        result = 31 * result + (f1 != +0.0f ? Float.floatToIntBits(f1) : 0);
        result = 31 * result + (str != null ? str.hashCode() : 0);
        result = 31 * result + (list != null ? list.hashCode() : 0);
        result = 31 * result + (set != null ? set.hashCode() : 0);
        result = 31 * result + (map != null ? map.hashCode() : 0);
        result = 31 * result + (bigDecimal != null ? bigDecimal.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }
}
