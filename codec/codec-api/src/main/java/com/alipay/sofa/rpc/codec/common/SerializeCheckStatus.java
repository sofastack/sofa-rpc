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
package com.alipay.sofa.rpc.codec.common;

/**
 * @author Even
 * @date 2024/1/5 19:12
 */
public enum SerializeCheckStatus {
    /**
     * Disable serialize check for all classes
     */
    DISABLE(0),

    /**
     * Only deny danger classes, warn if other classes are not in allow list
     */
    WARN(1),

    /**
     * Only allow classes in allow list, deny if other classes are not in allow list
     */
    STRICT(2);

    private final int mode;

    SerializeCheckStatus(int mode) {
        this.mode = mode;
    }

    public int getSerializeCheckMode() {
        return mode;
    }
}
