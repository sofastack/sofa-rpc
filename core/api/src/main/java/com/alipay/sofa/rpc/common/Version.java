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
package com.alipay.sofa.rpc.common;

/**
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @since 5.1.0
 * //FIXME 每次发布修改
 */
public final class Version {

    /**
     * 当前RPC版本，例如：5.6.7
     */
    public static final String VERSION       = "5.7.7";

    /**
     * 当前RPC版本，例如： 5.6.7 对应 50607
     */
    public static final int    RPC_VERSION   = 50707;

    /**
     * 当前Build版本，每次发布修改
     */
    public static final String BUILD_VERSION = "5.7.7_20210129173053";

}
