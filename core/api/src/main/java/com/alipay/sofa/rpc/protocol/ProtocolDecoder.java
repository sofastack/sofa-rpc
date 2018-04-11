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

import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;

/**
 * <p>协议解码器（注意，解码器应该不进行调用ByteBuf参数的释放，除非是解码过程中自己生产的ByteBuf）</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible
@Unstable
public abstract class ProtocolDecoder {

    /**
     * 协议基本信息
     */
    protected final ProtocolInfo protocolInfo;

    /**
     * 构造函数
     *
     * @param protocolInfo 协议基本信息
     */
    public ProtocolDecoder(ProtocolInfo protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    /**
     * 头部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     * @return decode result
     */
    public abstract Object decodeHeader(AbstractByteBuf byteBuf, Object out);

    /**
     * body解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     * @return decode result
     */
    public abstract Object decodeBody(AbstractByteBuf byteBuf, Object out);

    /**
     * 全部解码
     *
     * @param byteBuf 字节缓冲器
     * @param out     解析处理的对象列表
     * @return decode result
     */
    public abstract Object decodeAll(AbstractByteBuf byteBuf, Object out);
}
