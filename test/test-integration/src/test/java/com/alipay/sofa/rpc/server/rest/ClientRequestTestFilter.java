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

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Provider
@Priority(150)
public class ClientRequestTestFilter implements ClientRequestFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientRequestTestFilter.class);

    private static String       name   = "X";

    @CustomerAnnotation()
    private static String       code   = "x";

    private static String       code2  = "x";

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        LOGGER.info("客户端request filter生效");
        name = "A";
        LOGGER.info("客户端customerAnnotation  code:" + code);
    }

    @CustomerAnnotation()
    public void setCode(String code2) {
        this.code2 = code2;
        LOGGER.info("客户端customerAnnotation  code2:" + this.code2);

    }

    public static String getName() {
        return name + code + code2;
    }
}