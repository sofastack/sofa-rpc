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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transport.ServerTransportConfig;

/**
 * Http2WithSSL Server Transport
 * 
 * @author <a href="mailto:466178395@qq.com">LiHao</a>
 * @since 5.6.2
 */
@Extension("h2")
public class Http2WithSSLServerTransport extends AbstractHttp2ServerTransport {

    /**
     * 构造函数
     *
     * @param transportConfig
     *            服务端配置
     */
    protected Http2WithSSLServerTransport(ServerTransportConfig transportConfig) {
        super(transportConfig);
    }
}
