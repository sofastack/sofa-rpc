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

/**
 * constants of the polaris registry
 *
 * @author <a href=mailto:bner666@gmail.com>ZhangLibin</a>
 */
public class PolarisConstants {

    public static final String POLARIS_SERVER_CONNECTOR_PROTOCOL_KEY = "connector.protocol";

    public static final String HEALTH_CHECK_TTL_KEY                  = "healthCheck.ttl";

    public static final String HEARTBEAT_INTERVAL_KEY                = "heartbeat.interval";

    public static final String HEARTBEAT_CORE_SIZE_KEY               = "heartbeat.coreSize";

    public static final String LOOKUP_INTERVAL_KEY                   = "lookup.interval";

    public static final String POLARIS_SERVER_CONNECTOR_PROTOCOL     = "grpc";

    public static final int    DEFAULT_HEALTH_CHECK_TTL              = 10;

    public static final int    DEFAULT_HEARTBEAT_INTERVAL            = 3000;

    public static final int    DEFAULT_HEARTBEAT_CORE_SIZE           = 1;

    public static final int    DEFAULT_LOOKUP_INTERVAL               = 1000;
}
