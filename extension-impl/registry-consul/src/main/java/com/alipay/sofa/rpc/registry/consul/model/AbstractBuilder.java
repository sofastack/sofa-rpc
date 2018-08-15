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
package com.alipay.sofa.rpc.registry.consul.model;

import java.util.Map;

abstract class AbstractBuilder {

    private static final String        VARIABLE_START      = "${";
    private static final char          VARIABLE_END        = '}';
    private static final char          DEFAULT_VALUE_START = ':';

    private static Map<String, String> environment         = System.getenv();

    static void setEnvironmentForTesting(Map<String, String> environment) {
        AbstractBuilder.environment = environment;
    }

    protected String substituteEnvironmentVariables(String value) {
        // It might not look pretty, but this is actually about the fastest way to do it!
        final StringBuilder result = new StringBuilder();
        final int length = value.length();
        int index = 0;
        while (index < length) {
            final int start = value.indexOf(VARIABLE_START, index);
            if (start == -1) {
                result.append(value.substring(index));
                return result.toString();
            }
            final int end = value.indexOf(VARIABLE_END, start);
            if (end == -1) {
                result.append(value.substring(index));
                return result.toString();
            }
            if (start > index) {
                result.append(value.substring(index, start));
            }
            String defaultValue = null;
            String variable = value.substring(start + 2, end);
            final int split = variable.indexOf(DEFAULT_VALUE_START);
            if (split != -1) {
                defaultValue = variable.substring(split + 1);
                variable = variable.substring(0, split);
            }
            if (environment.containsKey(variable)) {
                result.append(environment.get(variable));
            } else if (defaultValue != null) {
                result.append(defaultValue);
            }
            index = end + 1;

        }
        return result.toString();
    }
}
