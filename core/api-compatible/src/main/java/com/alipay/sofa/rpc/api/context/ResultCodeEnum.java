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
package com.alipay.sofa.rpc.api.context;

/**
 * SOFA RPC response result code
 * 
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public enum ResultCodeEnum {

    /**
     * Success
     */
    SUCCESS("00"),
    /**
     * Catch exception when biz code
     */
    BIZ_FAILED("01"),
    /**
     * Catch exception when rpc code
     */
    RPC_FAILED("02"),
    /**
     * Client invoke timeout
     */
    TIMEOUT_FAILED("03"),
    /**
     * Catch excep
     */
    ROUTE_FAILED("04");

    private final String resultCode;

    private ResultCodeEnum(final String resultCode) {
        this.resultCode = resultCode;
    }

    public static ResultCodeEnum getResultCode(String resultCode) {
        if (resultCode != null) {
            for (ResultCodeEnum b : ResultCodeEnum.values()) {
                if (resultCode.equalsIgnoreCase(b.resultCode)) {
                    return b;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return resultCode;
    }

}
