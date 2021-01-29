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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author zhaowang
 * @version : RpcConfigKeysTest.java, v 0.1 2021年01月29日 1:28 下午 zhaowang
 */
public class RpcConfigKeysTest {

    @Test
    public void testDefaultValue() {
        Assert.assertEquals(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT));
        Assert.assertEquals(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT));
        Assert.assertEquals(RpcConfigKeys.TRACER_EXPOSE_TYPE.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.TRACER_EXPOSE_TYPE));
        Assert.assertEquals(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE));
        Assert.assertEquals(RpcConfigKeys.CERTIFICATE_PATH.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.CERTIFICATE_PATH));
        Assert.assertEquals(RpcConfigKeys.PRIVATE_KEY_PATH.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.PRIVATE_KEY_PATH));
        Assert.assertEquals(RpcConfigKeys.TRIPLE_EXPOSE_OLD_UNIQUE_ID_SERVICE.getDefaultValue(),
            SofaConfigs.getOrDefault(RpcConfigKeys.TRIPLE_EXPOSE_OLD_UNIQUE_ID_SERVICE));
    }
}