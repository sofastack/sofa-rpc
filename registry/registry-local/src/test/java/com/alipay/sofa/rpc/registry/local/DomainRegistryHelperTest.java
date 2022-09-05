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
package com.alipay.sofa.rpc.registry.local;

import org.junit.Test;

import static com.alipay.sofa.rpc.registry.local.DomainRegistryHelper.getDomain;
import static com.alipay.sofa.rpc.registry.local.DomainRegistryHelper.isDomain;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class DomainRegistryHelperTest {

    @Test
    public void testIsDomain() {
        assertTrue(isDomain("bolt://alipay.com:80?a=b"));
        assertTrue(isDomain("alipay.com:80?a=b"));
        assertTrue(isDomain("bolt://alipay.com:80"));
        assertTrue(isDomain("alipay.com:80"));
        assertTrue(isDomain("bolt://alipay?a=b"));
        assertTrue(isDomain("alipay"));
        assertTrue(isDomain("sofagw-pool"));

        assertFalse(isDomain("bolt://1.1.1.1:80?a=b"));
        assertFalse(isDomain("1.1.1.1:80?a=b"));
        assertFalse(isDomain("bolt://1.1.1.1:80"));
        assertFalse(isDomain("1.1.1.1:80"));
        assertFalse(isDomain("1.1.1.1"));

        //todo now we do not support ipv6
        //        assertFalse(isDomain("bolt://FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF#12200?a=b"));
        //        assertFalse(isDomain("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF#12200?a=b"));
        //        assertFalse(isDomain("bolt://FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF#12200"));
        //        assertFalse(isDomain("bolt://FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF?a=b"));
        //        assertFalse(isDomain("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"));
    }

    @Test
    public void testGetDomain() {
        assertEquals("alipay.com", getDomain("bolt://alipay.com:80?a=b"));
        assertEquals("alipay.com", getDomain("alipay.com:80?a=b"));
        assertEquals("alipay.com", getDomain("bolt://alipay.com:80"));
        assertEquals("alipay.com", getDomain("alipay.com:80"));
        assertEquals("alipay", getDomain("bolt://alipay?a=b"));
        assertEquals("alipay", getDomain("alipay"));
        assertEquals("sofagw-pool", getDomain("sofagw-pool"));

        assertEquals("1.1.1.1", getDomain("bolt://1.1.1.1:80?a=b"));
        assertEquals("1.1.1.1", getDomain("1.1.1.1:80?a=b"));
        assertEquals("1.1.1.1", getDomain("bolt://1.1.1.1:80"));
        assertEquals("1.1.1.1", getDomain("1.1.1.1:80"));
        assertEquals("1.1.1.1", getDomain("1.1.1.1"));
    }
}