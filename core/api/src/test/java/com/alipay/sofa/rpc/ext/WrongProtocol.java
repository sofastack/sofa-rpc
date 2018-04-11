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
package com.alipay.sofa.rpc.ext;

import com.alipay.sofa.rpc.protocol.Protocol;
import com.alipay.sofa.rpc.protocol.ProtocolDecoder;
import com.alipay.sofa.rpc.protocol.ProtocolEncoder;
import com.alipay.sofa.rpc.protocol.ProtocolInfo;
import com.alipay.sofa.rpc.protocol.ProtocolNegotiator;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "wp", code = -1)
public class WrongProtocol implements Protocol {

    @Override
    public ProtocolInfo protocolInfo() {
        return null;
    }

    @Override
    public ProtocolEncoder encoder() {
        return null;
    }

    @Override
    public ProtocolDecoder decoder() {
        return null;
    }

    @Override
    public ProtocolNegotiator negotiator() {
        return null;
    }
}
