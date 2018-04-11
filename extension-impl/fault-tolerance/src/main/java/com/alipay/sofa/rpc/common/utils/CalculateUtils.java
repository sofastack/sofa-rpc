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
package com.alipay.sofa.rpc.common.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 计算类
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class CalculateUtils {

    /**
     * 计算比率。计算结果四舍五入。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       保留小数点后位数
     * @return 比率
     */
    public static double divide(long numerator, long denominator, int scale) {
        BigDecimal numeratorBd = new BigDecimal(numerator);
        BigDecimal denominatorBd = new BigDecimal(denominator);
        return numeratorBd.divide(denominatorBd, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 计算比率。计算结果四舍五入。保留小数点后两位。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 比率
     */
    public static double divide(long numerator, long denominator) {
        return divide(numerator, denominator, 2);
    }

    /**
     * 计算比率。计算结果四舍五入。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       保留小数点后位数
     * @return 比率
     */
    public static double divide(double numerator, double denominator, int scale) {
        BigDecimal numeratorBd = new BigDecimal(Double.toString(numerator));
        BigDecimal denominatorBd = new BigDecimal(Double.toString(denominator));
        return numeratorBd.divide(denominatorBd, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 计算比率。计算结果四舍五入。保留小数点后两位。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 比率
     */
    public static double divide(double numerator, double denominator) {
        return divide(numerator, denominator, 2);
    }

    /**
     * 减法。计算结果四舍五入。
     *
     * @param minuend   被减数
     * @param reduction 减数
     * @param scale     计算结果保留位数。(注意包括整数部分)
     * @return 计算结果
     */
    public static double subtract(double minuend, double reduction, int scale) {
        BigDecimal minuendBd = new BigDecimal(Double.toString(minuend));
        BigDecimal reductionBd = new BigDecimal(Double.toString(reduction));
        MathContext mathContext = new MathContext(scale, RoundingMode.HALF_UP);
        return minuendBd.subtract(reductionBd, mathContext).doubleValue();
    }

    /**
     * 减法。
     *
     * @param minuend   被减数
     * @param reduction 减数
     * @return 计算结果
     */
    public static double subtract(double minuend, double reduction) {
        BigDecimal minuendBd = new BigDecimal(Double.toString(minuend));
        BigDecimal reductionBd = new BigDecimal(Double.toString(reduction));
        return minuendBd.subtract(reductionBd).doubleValue();
    }

    /**
     * 将int整数与小数相乘，计算结四舍五入保留整数位。
     *
     * @param num1 数字1
     * @param num2 数字2
     * @return 数字相乘计算结果
     */
    public static int multiply(int num1, double num2) {
        double num1D = num1;
        return multiply(num1D, num2);
    }

    /**
     * 将long整数与小数相乘，计算结四舍五入保留整数位。
     *
     * @param num1 数字1
     * @param num2 数字2
     * @return 数字相乘计算结果
     */
    public static int multiply(long num1, double num2) {
        double num1D = ((Long) num1).doubleValue();
        return multiply(num1D, num2);
    }

    /**
     * 将double与小数相乘，计算结四舍五入保留整数位。
     *
     * @param num1 数字1
     * @param num2 数字2
     * @return 数字相乘计算结果
     */
    public static int multiply(double num1, double num2) {
        BigDecimal num1Bd = new BigDecimal(Double.toString(num1));
        BigDecimal num2Bd = new BigDecimal(Double.toString(num2));
        MathContext mathContext = new MathContext(num1Bd.precision(), RoundingMode.HALF_UP);
        return num1Bd.multiply(num2Bd, mathContext).intValue();
    }

}