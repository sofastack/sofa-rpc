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

import com.alipay.sofa.common.config.ConfigKey;

/**
 * @author bystander
 * @version : DynamicConfigKeys.java, v 0.1 2019年04月17日 21:51 bystander Exp $
 */
public class DynamicConfigKeys {
    public static final String      DYNAMIC_ALIAS     = "dynamicAlias";

    public static final String      CONFIG_NODE       = "config";

    public static final String      DEFAULT_NAMESPACE = "sofa-rpc";

    public static ConfigKey<String> DYNAMIC_URL       = ConfigKey
                                                          .build(
                                                              "dynamicUrl",
                                                              " ",
                                                              false,
                                                              "The url of the dynamic configuration.",
                                                              new String[] { "dynamicUrl" });

    public static ConfigKey<String> ZK_ADDRESS        = ConfigKey
                                                          .build(
                                                              "sofa.rpc.config.center.zookeeper.address",
                                                              "127.0.0.1:2181",
                                                              false,
                                                              "The address of Zookeeper configuration center.",
                                                              new String[] { "zookeeper_address" });

    public static ConfigKey<String> NACOS_ADDRESS     = ConfigKey
                                                          .build(
                                                              "sofa.rpc.config.center.nacos.address",
                                                              "127.0.0.1:8848",
                                                              false,
                                                              "The address of Nacos configuration center.",
                                                              new String[] { "nacos_address" });
    public static ConfigKey<String> APOLLO_ADDRESS    = ConfigKey
                                                          .build(
                                                              "sofa.rpc.config.center.apollo.address",
                                                              "127.0.0.1:8080",
                                                              false,
                                                              "The address of Apollo configuration center.",
                                                              new String[] { "apollo_address" });

}