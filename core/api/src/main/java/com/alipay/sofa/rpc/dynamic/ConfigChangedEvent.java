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
package com.alipay.sofa.rpc.dynamic;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Narziss
 * @version ConfigChangedEvent.java, v 0.1 2024年09月15日 20:12 Narziss
 */

public class ConfigChangedEvent extends EventObject {

    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigChangedEvent.class);

    private final String key;

    private final String content;

    private final ConfigChangeType changeType;

    private final Map<String, String> dynamicValueMap = new HashMap<>();

    public ConfigChangedEvent(String key, String content) {
        this(key, content, ConfigChangeType.MODIFIED);
    }

    public ConfigChangedEvent(String key, String content, ConfigChangeType changeType) {
        super(key);
        this.key = key;
        this.content = content;
        this.changeType = changeType;
        if (StringUtils.isNotBlank(content)) {
            parseConfigurationLines(content);
        }
    }

    private void parseConfigurationLines(String content) {
        String[] lines = content.split(System.lineSeparator());
        for (String line : lines) {
            String[] keyValue = line.split("=", 2);
            if (keyValue.length == 2) {
                String mapKey = keyValue[0].trim();
                String mapValue = keyValue[1].trim();
                dynamicValueMap.put(mapKey, mapValue);
            } else {
                LOGGER.warn("Malformed configuration line: {}", line);
            }
        }
    }

    public String getKey() {
        return key;
    }

    public String getContent() {
        return content;
    }

    public ConfigChangeType getChangeType() {
        return changeType;
    }

    public Map<String, String> getDynamicValueMap() {
        return dynamicValueMap;
    }

}
