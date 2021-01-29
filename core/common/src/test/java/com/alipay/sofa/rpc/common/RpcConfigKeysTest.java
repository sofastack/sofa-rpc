/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
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
        Assert.assertEquals(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT));
        Assert.assertEquals(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT));
        Assert.assertEquals(RpcConfigKeys.TRACER_EXPOSE_TYPE.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.TRACER_EXPOSE_TYPE));
        Assert.assertEquals(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE));
        Assert.assertEquals(RpcConfigKeys.CERTIFICATE_PATH.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.CERTIFICATE_PATH));
        Assert.assertEquals(RpcConfigKeys.PRIVATE_KEY_PATH.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.PRIVATE_KEY_PATH));
        Assert.assertEquals(RpcConfigKeys.TRIPLE_EXPOSE_OLD_UNIQUE_ID_SERVICE.getDefaultValue(), SofaConfigs.getOrDefault(RpcConfigKeys.TRIPLE_EXPOSE_OLD_UNIQUE_ID_SERVICE));
    }
}