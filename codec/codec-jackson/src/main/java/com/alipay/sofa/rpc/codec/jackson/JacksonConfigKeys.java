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
package com.alipay.sofa.rpc.codec.jackson;

import com.alipay.sofa.common.config.ConfigKey;

/**
 *
 * @author junyuan
 * @version JacksonConfigKeys.java, v 0.1 2024年01月15日 15:25 junyuan Exp $
 */
public class JacksonConfigKeys {
    public static ConfigKey<String> JACKSON_SER_FEATURE_ENABLE_LIST  = ConfigKey
                                                                         .build(
                                                                             "sofa.rpc.codec.jackson.serialize.feature.enable.list",
                                                                             "",
                                                                             false,
                                                                             "希望被设置为开启的Serialize FEATURE",
                                                                             new String[] { "sofa_rpc_codec_jackson_serialize_feature_enable_list" });

    public static ConfigKey<String> JACKSON_SER_FEATURE_DISABLE_LIST = ConfigKey
                                                                         .build(
                                                                             "sofa.rpc.codec.jackson.serialize.feature.disable.list",
                                                                             "",
                                                                             false,
                                                                             "希望被设置为关闭的Serialize FEATURE",
                                                                             new String[] { "sofa_rpc_codec_jackson_serialize_feature_disable_list" });

    public static ConfigKey<String> JACKSON_DES_FEATURE_ENABLE_LIST  = ConfigKey
                                                                         .build(
                                                                             "sofa.rpc.codec.jackson.deserialize.feature.enable.list",
                                                                             "",
                                                                             false,
                                                                             "希望被设置为开启的Deserialize FEATURE",
                                                                             new String[] { "sofa_rpc_codec_jackson_deserialize_feature_disable_list" });

    public static ConfigKey<String> JACKSON_DES_FEATURE_DISABLE_LIST = ConfigKey
                                                                         .build(
                                                                             "sofa.rpc.codec.jackson.deserialize.feature.disable.list",
                                                                             "",
                                                                             false,
                                                                             "希望被设置为关闭的Deserialize FEATURE",
                                                                             new String[] { "sofa_rpc_codec_jackson_deserialize_feature_disable_list" });
}