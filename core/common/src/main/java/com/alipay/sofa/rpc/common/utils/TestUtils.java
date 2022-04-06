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

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zhaowang
 * @version : TestUtils.java, v 0.1 2022年01月25日 5:00 下午 zhaowang
 */
public class TestUtils {

    public static String randomString() {
        int length = 10;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int offset = ThreadLocalRandom.current().nextInt(26 * 2);
            char targetChr = (char) ('A' + offset);
            if (targetChr > 'Z') {
                targetChr = (char) (targetChr + ('a' - 'Z' - 1));
            }
            sb.append(targetChr);
        }
        return sb.toString();
    }
}