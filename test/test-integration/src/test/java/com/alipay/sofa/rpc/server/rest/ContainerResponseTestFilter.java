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

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Provider
public class ContainerResponseTestFilter implements ContainerResponseFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(ContainerResponseTestFilter.class);

    private static String       name   = "X";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        LOGGER.info("服务端response filter生效");
        name = "C";

    }

    public static String getName() {
        return name;
    }
}