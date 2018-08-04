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

    public static final String INTERFACE_KEY         = "interface";
    public static final String INTERFACECLASS_KEY    = "interfaceClass";
    public static final String GENERIC_KEY           = "generic";
    public static final String GROUP_KEY             = "uniqueId";
    public static final String VERSION_KEY           = "version";
    public static final String REGISTRY_RPC_PORT_KEY = "registryrpcport";
    public static final String HTTP_PORT_KEY         = "httpport";
    public static final String MONITOR_INTERVAL      = "monitorinterval";
    public static final String APPLICATION_NAME      = "application";
    public static final String TIMEOUT               = "timeout";
    public static final String DEFAULT_GROUP         = "Default";
    public static final String DEFAULT_VERSION       = "1.0.0";
    public static final String LOCALHOST_KEY         = "localhost";
    public static final String ANYHOST_KEY           = "anyhost";
    public static final String RETRY_METHODS_KEY     = "retrymethods";
    public static final String FALLBACK_METHODS_KEY  = "fallbackmethods";
    public static final String METHOD_KEY            = "method";
    public static final String METHOD_RETRY_KEY      = "retries";
    public static final String ANYHOST_VALUE         = "0.0.0.0";

    public static final String REGISTRY_RETRY_PERIOD_KEY     = "retry.period";
    public static final int    DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;
    public static final String ENABLED_KEY                   = "enabled";
    public static final String DISABLED_KEY                  = "disabled";

    public static final String CONSUL_SERVICE_PRE = "consul_";
    public static final String PATH_SEPARATOR     = "/";
    public static final String PROVIDERS_CATEGORY = "providers";
    public static final String CONSUMERS_CATEGORY = "consumers";

    public static final String REGISTRY_PROTOCOL = "registry";
    public static final String REMOTE_PROTOCOL   = "grpc";
    public static final String MONITOR_PROTOCOL  = "monitor";

    public static final String ASYNC_KEY                 = "async";
    public static final int    RPCTYPE_ASYNC             = 1;
    public static final int    RPCTYPE_BLOCKING          = 2;
    public static final int    RPC_ASYNC_DEFAULT_TIMEOUT = 5000;

    public static final String REMOTE_ADDRESS = "remote";

    public static final String VALIDATOR_GROUPS = "validator.groups";

}
