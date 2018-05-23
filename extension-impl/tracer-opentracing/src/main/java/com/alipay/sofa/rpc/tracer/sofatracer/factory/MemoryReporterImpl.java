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
package com.alipay.sofa.rpc.tracer.sofatracer.factory;

import com.alipay.common.tracer.core.appender.encoder.SpanEncoder;
import com.alipay.common.tracer.core.reporter.facade.AbstractReporter;
import com.alipay.common.tracer.core.reporter.stat.SofaTracerStatisticReporter;
import com.alipay.common.tracer.core.reporter.stat.model.StatKey;
import com.alipay.common.tracer.core.reporter.stat.model.StatValues;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.annotation.JustForTest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * don't use for production
 * @author bystander
 * @version $Id: MemoryReporterImpl.java, v 0.1 2018年05月17日 9:10 AM bystander Exp $
 */
@JustForTest
public class MemoryReporterImpl extends AbstractReporter {

    private List<String>                stats      = new ArrayList<String>();

    private SofaTracerStatisticReporter statReporter;

    private Map<StatKey, StatValues>    storeDatas = new HashMap<StatKey, StatValues>();

    public MemoryReporterImpl(String digestLog, String digestRollingPolicy, String digestLogReserveConfig,
                              SpanEncoder<SofaTracerSpan> spanEncoder, SofaTracerStatisticReporter statReporter) {
        this.statReporter = statReporter;
    }

    @Override
    public void doReport(SofaTracerSpan span) {
        stats.add(span.toString());

        if (statReporter != null) {
            statisticReport(span);
        }
    }

    @Override
    public String getReporterType() {
        return "Memory";
    }

    private void statisticReport(SofaTracerSpan span) {

        statReporter.reportStat(span);
        Field statDatas;
        Map<StatKey, StatValues> datas = null;
        try {
            statDatas = getDeclaredField(statReporter, "statDatas");
            statDatas.setAccessible(true);
            datas = (Map<StatKey, StatValues>) statDatas.get(statReporter);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        storeDatas.putAll(datas);

    }

    public Map<StatKey, StatValues> getStoreDatas() {
        return storeDatas;
    }

    /**
     * 循环向上转型, 获取对象的 DeclaredField
     *
     * @param object    : 子类对象
     * @param fieldName : 父类中的属性名
     * @return 父类中的属性对象
     */
    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {
                //这里甚么都不要做！并且这里的异常必须这样写，不能抛出去。
                //如果这里的异常打印或者往外抛，则就不会执行clazz = clazz.getSuperclass(),最后就不会进入到父类中了
            }
        }
        return null;
    }
}