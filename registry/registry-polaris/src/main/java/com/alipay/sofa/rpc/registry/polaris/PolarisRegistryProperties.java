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

import static com.alipay.sofa.rpc.registry.polaris.PolarisConstants.*;

public class PolarisRegistryProperties {
    private final Map<String, String> registryParameters;

    public PolarisRegistryProperties(Map<String, String> registryParameters) {
        if (registryParameters == null) {
            registryParameters = Collections.emptyMap();
        }
        this.registryParameters = registryParameters;
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

    public int getWatchTimeout() {
        return getInt(WATCH_TIMEOUT_KEY, DEFAULT_WATCH_TIMEOUT);
    }



    public String getHealthCheckHost(String host) {
        return getString(HEALTH_CHECK_HOST_KEY, host);
    }

    public int getHealthCheckPort(int port) {
        return getInt(HEALTH_CHECK_PORT_KEY, port);
    }

    public String getHealthCheckTimeout() {
        return getString(HEALTH_CHECK_TIMEOUT_KEY, DEFAULT_HEALTH_CHECK_TIMEOUT);
    }

    public String getHealthCheckInterval() {
        return getString(HEALTH_CHECK_INTERVAL_KEY, DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    public String getHealthCheckProtocol() {
        return getString(HEALTH_CHECK_PROTOCOL_KEY, DEFAULT_HEALTH_CHECK_PROTOCOL);
    }

    public String getHealthCheckPath() {
        return getString(HEALTH_CHECK_PATH_KEY, DEFAULT_HEALTH_CHECK_PATH);
    }

    public String getHealthCheckMethod() {
        return getString(HEALTH_CHECK_METHOD_KEY, DEFAULT_HEALTH_CHECK_METHOD);
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

    public enum HealthCheckType {
        TTL, TCP, HTTP
    }
}
