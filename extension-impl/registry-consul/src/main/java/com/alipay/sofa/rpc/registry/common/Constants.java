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
package com.alipay.sofa.rpc.registry.common;

import java.util.regex.Pattern;

/**
 * Constants class for Consul Registry
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class Constants {

    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    public static final String  DEFAULT_VERSION     = "1.0.0";
    public static final String  LOCALHOST_KEY       = "localhost";
    public static final String  ANYHOST_KEY         = "anyhost";
    public static final String  ANYHOST_VALUE       = "0.0.0.0";

    public static final String  CONSUL_SERVICE_PRE  = "consul_";
    public static final String  PATH_SEPARATOR      = "/";
    public static final String  PROVIDERS_CATEGORY  = "providers";
    public static final String  CONSUMERS_CATEGORY  = "consumers";

}
