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
package com.alipay.sofa.rpc.bench;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.struct.ListDifference;
import com.alipay.sofa.rpc.common.struct.SetDifference;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author xiaojian.xj
 * @version : CollectionDifferenceBenchMarkTest.java, v 0.1 2022年09月06日 21:45 xiaojian.xj Exp $
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(5)
@Threads(1)
@State(value = Scope.Benchmark)
public class CollectionDifferenceBenchMarkTest {

    private static final String URL_PATTERN = "%s:12200?rpcVer=50803&serialization=hessian2&weight=100&timeout=3000&appName=testApp&p=1&v=4.0&_SERIALIZETYPE=hessian2&_WEIGHT=100&_TIMEOUT=3000&app_name=testApp";

    private static final List<ProviderInfo> EXIST = new ArrayList<>();

    private static final List<ProviderInfo> UPDATE = new ArrayList<>();

    private static Set<ProviderInfo> EXIST_SET;

    private static Set<ProviderInfo> UPDATE_SET;

    private static final String TO_BE_ADD =  String.format(URL_PATTERN, getRandomIp());

    private static final String TO_BE_REMOVE =  String.format(URL_PATTERN, getRandomIp());

    @Param(value = {"1000", "5000", "10000", "20000"})
    private int LENGTH;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CollectionDifferenceBenchMarkTest.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .result("result.json")
                .resultFormat(ResultFormatType.JSON).build();
        new Runner(opt).run();
    }

    @Benchmark
    public void testListDifference() {
        ListDifference difference = new ListDifference(EXIST, UPDATE);
        Assert.assertEquals(difference.getOnBoth().size(), LENGTH);
        Assert.assertEquals(difference.getOnlyOnLeft().size(), 1);
        Assert.assertEquals(difference.getOnlyOnRight().size(), 1);
    }

    @Benchmark
    public void testSetDifference() {
        SetDifference difference = new SetDifference(EXIST_SET, UPDATE_SET);
        Assert.assertEquals(difference.getOnBoth().size(), LENGTH);
        Assert.assertEquals(difference.getOnlyOnLeft().size(), 1);
        Assert.assertEquals(difference.getOnlyOnRight().size(), 1);
    }

    @Setup
    public void prepareData() {
        Set<String> existUrl = new HashSet<>();
        for (int i = 0; i < LENGTH; i++) {
            String url = String.format(URL_PATTERN, getRandomIp());;

            while (existUrl.contains(url)) {
                url = String.format(URL_PATTERN, getRandomIp());
            }
            existUrl.add(url);
            EXIST.add(ProviderHelper.toProviderInfo(url));
            UPDATE.add(ProviderHelper.toProviderInfo(url));
        }
        EXIST.add(ProviderHelper.toProviderInfo(TO_BE_REMOVE));
        UPDATE.add(ProviderHelper.toProviderInfo(TO_BE_ADD));

        EXIST_SET = new HashSet<>(EXIST);
        UPDATE_SET = new HashSet<>(UPDATE);
    }

    @TearDown
    public void clean() {
        EXIST.clear();
        UPDATE.clear();
    }

    public static String getRandomIp() {
        Random r = new Random();
        return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
    }
}
