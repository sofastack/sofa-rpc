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
package com.alipay.sofa.rpc.proxy;

import java.util.HashMap;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TestInterfaceImpl implements TestInterface {

    @Override
    public String sayHello(String s) {
        return "sayHello";
    }

    @Override
    public void sayNoting() {

    }

    @Override
    public int sayNum(boolean s) {
        return 678;
    }

    @Override
    public int[] sayNums(List list, HashMap map) {
        return new int[] { 6, 7, 8 };
    }

    @Override
    public Float sayNum2(Double list) {
        return null;
    }

    @Override
    public String throwbiz1() {
        throw new RuntimeException("RuntimeException");
    }

    @Override
    public String throwbiz2() throws Throwable {
        throw new Throwable("Throwable");
    }

    @Override
    public String throwRPC() {
        return null;
    }
}
