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
package com.alipay.sofa.rpc.client.router;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for DirectUrlRouter
 *
 * @author SOFA-RPC Team
 */
public class DirectUrlRouterTest {

    @Test
    public void testClassInitialization() {
        Assert.assertNotNull(DirectUrlRouter.class);
    }

    @Test
    public void testExtensionAnnotation() {
        DirectUrlRouter router = new DirectUrlRouter();
        com.alipay.sofa.rpc.ext.Extension extension =
                router.getClass().getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);

        Assert.assertNotNull(extension);
        Assert.assertEquals("directUrl", extension.value());
        Assert.assertEquals(-20000, extension.order());
    }

    @Test
    public void testRouterConstant() {
        Assert.assertEquals("DIRECT", DirectUrlRouter.RPC_DIRECT_URL_ROUTER);
    }

    @Test
    public void testRouterInstantiation() {
        DirectUrlRouter router = new DirectUrlRouter();
        Assert.assertNotNull(router);
    }
}
