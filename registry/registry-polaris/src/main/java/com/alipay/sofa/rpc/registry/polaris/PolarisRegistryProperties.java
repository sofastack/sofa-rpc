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

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.DEFAULT_HEALTH_CHECK_TTL;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.DEFAULT_HEARTBEAT_CORE_SIZE;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.DEFAULT_HEARTBEAT_INTERVAL;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.DEFAULT_LOOKUP_INTERVAL;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.HEALTH_CHECK_TTL_KEY;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.HEARTBEAT_CORE_SIZE_KEY;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.HEARTBEAT_INTERVAL_KEY;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.LOOKUP_INTERVAL_KEY;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.POLARIS_SERVER_CONNECTOR_PROTOCOL;
import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.POLARIS_SERVER_CONNECTOR_PROTOCOL_KEY;

/**
 * the properties of polaris registry
 *
 * @author <a href=mailto:bner666@gmail.com>ZhangLibin</a>
 */
public class PolarisRegistryProperties {
    private final Map<String, String> registryParameters;

    public PolarisRegistryProperties(Map<String, String> registryParameters) {
        if (registryParameters == null) {
            registryParameters = Collections.emptyMap();
        }
        this.registryParameters = registryParameters;
    }

    public String getConnectorProtocol() {
        return getString(POLARIS_SERVER_CONNECTOR_PROTOCOL_KEY, POLARIS_SERVER_CONNECTOR_PROTOCOL);
    }

    public int getHealthCheckTTL() {
        return getInt(HEALTH_CHECK_TTL_KEY, DEFAULT_HEALTH_CHECK_TTL);
    }

    public int getHeartbeatInterval() {
        return getInt(HEARTBEAT_INTERVAL_KEY, DEFAULT_HEARTBEAT_INTERVAL);
    }

    public int getHeartbeatCoreSize() {
        return getInt(HEARTBEAT_CORE_SIZE_KEY, DEFAULT_HEARTBEAT_CORE_SIZE);
    }

    public int getLookupInterval() {
        return getInt(LOOKUP_INTERVAL_KEY, DEFAULT_LOOKUP_INTERVAL);
    }

    private int getInt(String key, int defaultValue) {
        return get(key, Integer::parseInt, defaultValue);
    }

    private String getString(String key, String defaultValue) {
        return get(key, Function.identity(), defaultValue);
    }

    private <T> T get(String key, Function<String, T> transform, T defaultValue) {
        String value = registryParameters.get(key);
        if (value != null) {
            return transform.apply(value);
        }
        return defaultValue;
    }

}
