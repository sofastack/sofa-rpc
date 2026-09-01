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
package com.alipay.sofa.rpc.profile.jfr;

/**
 * SOFA RPC JFR事件名称。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
public final class JfrEventNames {

    public static final String CLIENT_INVOCATION = "com.alipay.sofa.rpc.ClientInvocation";

    public static final String SERVER_INVOCATION = "com.alipay.sofa.rpc.ServerInvocation";

    private JfrEventNames() {
    }
}
