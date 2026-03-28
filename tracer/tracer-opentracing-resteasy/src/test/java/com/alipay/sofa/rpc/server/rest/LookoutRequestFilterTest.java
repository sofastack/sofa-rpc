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
package com.alipay.sofa.rpc.server.rest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for LookoutRequestFilter
 *
 * @author SOFA-RPC Team
 */
public class LookoutRequestFilterTest {

    @Test
    public void testFilterCreation() {
        LookoutRequestFilter filter = new LookoutRequestFilter();
        assertNotNull(filter);
    }

    @Test
    public void testFilterAnnotation() {
        // Verify JAX-RS annotations
        assertNotNull(LookoutRequestFilter.class.getAnnotation(javax.ws.rs.ext.Provider.class));
        assertNotNull(LookoutRequestFilter.class.getAnnotation(javax.annotation.Priority.class));

        javax.annotation.Priority priority =
                LookoutRequestFilter.class.getAnnotation(javax.annotation.Priority.class);
        assertEquals(100, priority.value());
    }
}
