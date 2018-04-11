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
package com.alipay.sofa.rpc.protocol;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TestProtocolInfo extends ProtocolInfo {

    public TestProtocolInfo() {
        super("xx", (byte) 112, true, NET_PROTOCOL_TCP);
    }

    @Override
    public int maxFrameLength() {
        return 0;
    }

    @Override
    public int lengthFieldOffset() {
        return 0;
    }

    @Override
    public int lengthFieldLength() {
        return 0;
    }

    @Override
    public int lengthAdjustment() {
        return 0;
    }

    @Override
    public int initialBytesToStrip() {
        return 0;
    }

    @Override
    public int magicFieldLength() {
        return 0;
    }

    @Override
    public int magicFieldOffset() {
        return 0;
    }

    @Override
    public byte[] magicCode() {
        return new byte[0];
    }

    @Override
    public boolean isMatchMagic(byte[] bs) {
        return false;
    }
}
