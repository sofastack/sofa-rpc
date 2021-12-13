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
package com.alipay.sofa.rpc.registry.polaris;

public class PolarisConstants {

    //GlobalConfig  ServerConnectorConfig
    public static final String POLARIS_SERVER_CONNECTOR_ADDRESS  = "consulServiceName";
    public static final String POLARIS_SERVER_CONNECTOR_PROTOCOL = "consulServiceName";

    //GlobalConfig  SystemConfig
    //获取服务发现集群地址
    //获取心跳集群地址

    //GlobalConfig APIConfig
    //api.timeout
    //api.maxRetryTimes
    //api.reportInterval
    //api.retryInterval

    //GlobalConfig  StatReporterConfig

    public static final String CONSUL_SERVICE_NAME_KEY           = "consulServiceName";

    public static final String HEARTBEAT_INTERVAL_KEY            = "heartbeat.interval";

    public static final String HEARTBEAT_CORE_SIZE_KEY           = "heartbeat.coreSize";

    public static final String LOOKUP_INTERVAL_KEY               = "lookup.interval";

    public static final String WATCH_TIMEOUT_KEY                 = "watch.timeout";

    public static final String HEALTH_CHECK_HOST_KEY             = "healthCheck.host";

    public static final String HEALTH_CHECK_PORT_KEY             = "healthCheck.port";

    public static final String HEALTH_CHECK_TIMEOUT_KEY          = "healthCheck.timeout";

    public static final String HEALTH_CHECK_INTERVAL_KEY         = "healthCheck.interval";

    public static final String HEALTH_CHECK_PROTOCOL_KEY         = "healthCheck.protocol";

    public static final String HEALTH_CHECK_PATH_KEY             = "healthCheck.path";

    public static final String HEALTH_CHECK_METHOD_KEY           = "healthCheck.method";

    public static final int    DEFAULT_POLARIS_PORT              = 8090;

    public static final int    DEFAULT_HEARTBEAT_INTERVAL        = 3000;

    public static final int    DEFAULT_HEARTBEAT_CORE_SIZE       = 1;

    public static final int    DEFAULT_LOOKUP_INTERVAL           = 1000;

    public static final int    DEFAULT_WATCH_TIMEOUT             = 5;

    public static final int    DEFAULT_HEALTH_CHECK_TTL          = 10;

    public static final String DEFAULT_HEALTH_CHECK_TIMEOUT      = "1s";

    public static final String DEFAULT_HEALTH_CHECK_INTERVAL     = "5s";

    public static final String DEFAULT_HEALTH_CHECK_PROTOCOL     = "http";

    public static final String DEFAULT_HEALTH_CHECK_PATH         = "/health";

    public static final String DEFAULT_HEALTH_CHECK_METHOD       = "GET";
}
