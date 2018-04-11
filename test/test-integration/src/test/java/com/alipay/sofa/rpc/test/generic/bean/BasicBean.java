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
package com.alipay.sofa.rpc.test.generic.bean;

/**
 * @author xuanbei
 * @since 2017/01/06
 */
public class BasicBean {
    private short   s;
    private Short   ss;

    private int     i;
    private Integer ii;

    private byte    b;
    private Byte    bb;

    private long    l;
    private Long    ll;

    private float   f;
    private Float   ff;

    private double  d;
    private Double  dd;

    private boolean bo;
    private Boolean bbo;

    public BasicBean(short s, Short ss, int i, Integer ii, byte b, Byte bb, long l, Long ll,
                     float f, Float ff, double d, Double dd, boolean bo, Boolean bbo) {
        this.s = s;
        this.ss = ss;
        this.i = i;
        this.ii = ii;
        this.b = b;
        this.bb = bb;
        this.l = l;
        this.ll = ll;
        this.f = f;
        this.ff = ff;
        this.d = d;
        this.dd = dd;
        this.bo = bo;
        this.bbo = bbo;
    }

    public short getS() {
        return s;
    }

    public void setS(short s) {
        this.s = s;
    }

    public Short getSs() {
        return ss;
    }

    public void setSs(Short ss) {
        this.ss = ss;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public Integer getIi() {
        return ii;
    }

    public void setIi(Integer ii) {
        this.ii = ii;
    }

    public byte getB() {
        return b;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public Byte getBb() {
        return bb;
    }

    public void setBb(Byte bb) {
        this.bb = bb;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public Long getLl() {
        return ll;
    }

    public void setLl(Long ll) {
        this.ll = ll;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public Float getFf() {
        return ff;
    }

    public void setFf(Float ff) {
        this.ff = ff;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public Double getDd() {
        return dd;
    }

    public void setDd(Double dd) {
        this.dd = dd;
    }

    public boolean isBo() {
        return bo;
    }

    public void setBo(boolean bo) {
        this.bo = bo;
    }

    public Boolean getBbo() {
        return bbo;
    }

    public void setBbo(Boolean bbo) {
        this.bbo = bbo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BasicBean basicBean = (BasicBean) o;

        if (s != basicBean.s)
            return false;
        if (i != basicBean.i)
            return false;
        if (b != basicBean.b)
            return false;
        if (l != basicBean.l)
            return false;
        if (Float.compare(basicBean.f, f) != 0)
            return false;
        if (Double.compare(basicBean.d, d) != 0)
            return false;
        if (bo != basicBean.bo)
            return false;
        if (ss != null ? !ss.equals(basicBean.ss) : basicBean.ss != null)
            return false;
        if (ii != null ? !ii.equals(basicBean.ii) : basicBean.ii != null)
            return false;
        if (bb != null ? !bb.equals(basicBean.bb) : basicBean.bb != null)
            return false;
        if (ll != null ? !ll.equals(basicBean.ll) : basicBean.ll != null)
            return false;
        if (ff != null ? !ff.equals(basicBean.ff) : basicBean.ff != null)
            return false;
        if (dd != null ? !dd.equals(basicBean.dd) : basicBean.dd != null)
            return false;
        return bbo != null ? bbo.equals(basicBean.bbo) : basicBean.bbo == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) s;
        result = 31 * result + (ss != null ? ss.hashCode() : 0);
        result = 31 * result + i;
        result = 31 * result + (ii != null ? ii.hashCode() : 0);
        result = 31 * result + (int) b;
        result = 31 * result + (bb != null ? bb.hashCode() : 0);
        result = 31 * result + (int) (l ^ (l >>> 32));
        result = 31 * result + (ll != null ? ll.hashCode() : 0);
        result = 31 * result + (f != +0.0f ? Float.floatToIntBits(f) : 0);
        result = 31 * result + (ff != null ? ff.hashCode() : 0);
        temp = Double.doubleToLongBits(d);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (dd != null ? dd.hashCode() : 0);
        result = 31 * result + (bo ? 1 : 0);
        result = 31 * result + (bbo != null ? bbo.hashCode() : 0);
        return result;
    }
}
