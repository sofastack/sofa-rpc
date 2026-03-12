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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for ResultCodeEnum
 *
 * @author SOFA-RPC Team
 */
public class ResultCodeEnumTest {

    @Test
    public void testEnumValues() {
        Assert.assertEquals(5, ResultCodeEnum.values().length);
    }

    @Test
    public void testSuccessCode() {
        Assert.assertEquals("00", ResultCodeEnum.SUCCESS.toString());
    }

    @Test
    public void testBizFailedCode() {
        Assert.assertEquals("01", ResultCodeEnum.BIZ_FAILED.toString());
    }

    @Test
    public void testRpcFailedCode() {
        Assert.assertEquals("02", ResultCodeEnum.RPC_FAILED.toString());
    }

    @Test
    public void testTimeoutFailedCode() {
        Assert.assertEquals("03", ResultCodeEnum.TIMEOUT_FAILED.toString());
    }

    @Test
    public void testRouteFailedCode() {
        Assert.assertEquals("04", ResultCodeEnum.ROUTE_FAILED.toString());
    }

    @Test
    public void testGetResultCode() {
        Assert.assertEquals(ResultCodeEnum.SUCCESS, ResultCodeEnum.getResultCode("00"));
        Assert.assertEquals(ResultCodeEnum.BIZ_FAILED, ResultCodeEnum.getResultCode("01"));
        Assert.assertEquals(ResultCodeEnum.RPC_FAILED, ResultCodeEnum.getResultCode("02"));
        Assert.assertEquals(ResultCodeEnum.TIMEOUT_FAILED, ResultCodeEnum.getResultCode("03"));
        Assert.assertEquals(ResultCodeEnum.ROUTE_FAILED, ResultCodeEnum.getResultCode("04"));
    }

    @Test
    public void testGetResultCodeCaseInsensitive() {
        Assert.assertEquals(ResultCodeEnum.SUCCESS, ResultCodeEnum.getResultCode("00"));
    }

    @Test
    public void testGetResultCodeNull() {
        Assert.assertNull(ResultCodeEnum.getResultCode(null));
    }

    @Test
    public void testGetResultCodeInvalid() {
        Assert.assertNull(ResultCodeEnum.getResultCode("99"));
        Assert.assertNull(ResultCodeEnum.getResultCode(""));
    }
}
