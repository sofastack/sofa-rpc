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
package com.alipay.sofa.rpc.tracer.sofatracer.code;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for TracerResultCode
 *
 * @author SOFA-RPC Team
 */
public class TracerResultCodeTest {

    @Test
    public void testResultCodeConstants() {
        Assert.assertEquals("00", TracerResultCode.RPC_RESULT_SUCCESS);
        Assert.assertEquals("01", TracerResultCode.RPC_RESULT_BIZ_FAILED);
        Assert.assertEquals("02", TracerResultCode.RPC_RESULT_RPC_FAILED);
        Assert.assertEquals("03", TracerResultCode.RPC_RESULT_TIMEOUT_FAILED);
        Assert.assertEquals("04", TracerResultCode.RPC_RESULT_ROUTE_FAILED);
    }

    @Test
    public void testErrorTypeConstants() {
        Assert.assertEquals("biz_error", TracerResultCode.RPC_ERROR_TYPE_BIZ_ERROR);
        Assert.assertEquals("address_route_error", TracerResultCode.RPC_ERROR_TYPE_ADDRESS_ROUTE_ERROR);
        Assert.assertEquals("serialize_error", TracerResultCode.RPC_ERROR_TYPE_SERIALIZE_ERROR);
        Assert.assertEquals("timeout_error", TracerResultCode.RPC_ERROR_TYPE_TIMEOUT_ERROR);
        Assert.assertEquals("unknown_error", TracerResultCode.RPC_ERROR_TYPE_UNKNOWN_ERROR);
    }

    @Test
    public void testClassInitialization() {
        Assert.assertNotNull(TracerResultCode.class);
    }
}
