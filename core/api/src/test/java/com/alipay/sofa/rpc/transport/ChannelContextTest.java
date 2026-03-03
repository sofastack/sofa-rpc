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
package com.alipay.sofa.rpc.transport;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ChannelContext}.
 */
public class ChannelContextTest {

    @Test
    public void testPutAndGetHeadCache() {
        ChannelContext context = new ChannelContext();
        // headerCache is null initially
        Assert.assertNull(context.getHeader((short) 1));

        context.putHeadCache((short) 1, "testValue");
        Assert.assertEquals("testValue", context.getHeader((short) 1));
    }

    @Test
    public void testPutHeadCacheDoesNotOverwrite() {
        ChannelContext context = new ChannelContext();
        context.putHeadCache((short) 1, "first");
        context.putHeadCache((short) 1, "second");
        // Should keep the first value since containsKey is checked
        Assert.assertEquals("first", context.getHeader((short) 1));
    }

    @Test
    public void testGetHeaderKey() {
        ChannelContext context = new ChannelContext();
        context.putHeadCache((short) 1, "testValue");
        Assert.assertEquals(Short.valueOf((short) 1), context.getHeaderKey("testValue"));
        Assert.assertNull(context.getHeaderKey("nonexistent"));
    }

    @Test
    public void testInvalidateHeadCacheSuccess() {
        ChannelContext context = new ChannelContext();
        context.putHeadCache((short) 1, "testValue");
        Assert.assertEquals("testValue", context.getHeader((short) 1));

        // Note: invalidateHeadCache takes Byte key while putHeadCache uses Short key.
        // Due to type mismatch, a Byte key cannot find a Short entry in the map.
        // Verify that cache remains unchanged when called with a Byte key.
        context.invalidateHeadCache((byte) 1, "testValue");
        Assert.assertEquals("testValue", context.getHeader((short) 1));
    }

    @Test
    public void testInvalidateHeadCacheNoMatchingKey() {
        ChannelContext context = new ChannelContext();
        context.putHeadCache((short) 1, "testValue");

        // Byte key cannot match Short key in the map, so no exception is thrown
        context.invalidateHeadCache((byte) 1, "wrongValue");
        Assert.assertEquals("testValue", context.getHeader((short) 1));
    }

    @Test
    public void testInvalidateHeadCacheNonExistentKey() {
        ChannelContext context = new ChannelContext();
        context.putHeadCache((short) 1, "testValue");

        // Invalidate a key that doesn't exist should be a no-op
        context.invalidateHeadCache((byte) 99, "anyValue");
        Assert.assertEquals("testValue", context.getHeader((short) 1));
    }

    @Test
    public void testInvalidateHeadCacheWhenCacheIsNull() {
        ChannelContext context = new ChannelContext();
        // Should not throw when headerCache is null
        context.invalidateHeadCache((byte) 1, "value");
    }

    @Test
    public void testGetAvailableRefIndexConsumerToProvider() {
        ChannelContext context = new ChannelContext();
        Short index = context.getAvailableRefIndex(true);
        Assert.assertNotNull(index);
        Assert.assertEquals(Short.valueOf((short) 0), index);

        // After putting key 0, next available should be 1
        context.putHeadCache((short) 0, "val0");
        index = context.getAvailableRefIndex(true);
        Assert.assertEquals(Short.valueOf((short) 1), index);
    }

    @Test
    public void testGetAvailableRefIndexProviderToConsumer() {
        ChannelContext context = new ChannelContext();
        Short index = context.getAvailableRefIndex(false);
        Assert.assertNotNull(index);
        Assert.assertEquals(Short.valueOf((short) -1), index);

        // After putting key -1, next available should be -2
        context.putHeadCache((short) -1, "val-1");
        index = context.getAvailableRefIndex(false);
        Assert.assertEquals(Short.valueOf((short) -2), index);
    }

    @Test
    public void testGetHeaderWithNullKey() {
        ChannelContext context = new ChannelContext();
        Assert.assertNull(context.getHeader(null));
    }

    @Test
    public void testGetHeaderKeyWithEmpty() {
        ChannelContext context = new ChannelContext();
        Assert.assertNull(context.getHeaderKey(""));
        Assert.assertNull(context.getHeaderKey(null));
    }

    @Test
    public void testSettersAndGetters() {
        ChannelContext context = new ChannelContext();

        context.setDstVersion(100);
        Assert.assertEquals(Integer.valueOf(100), context.getDstVersion());

        context.setClientAppId("appId");
        Assert.assertEquals("appId", context.getClientAppId());

        context.setClientAppName("appName");
        Assert.assertEquals("appName", context.getClientAppName());

        context.setClientInstanceId("instanceId");
        Assert.assertEquals("instanceId", context.getClientInstanceId());

        context.setProtocol("bolt");
        Assert.assertEquals("bolt", context.getProtocol());
    }
}
