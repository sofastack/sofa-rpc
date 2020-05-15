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

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoader;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderListener;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory of protocol
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ProtocolFactory {

    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外<br>
     * 还需要额外保留编码和实例的映射：{编码：协议}
     */
    private final static ConcurrentMap<Byte, Protocol> TYPE_PROTOCOL_MAP = new ConcurrentHashMap<Byte, Protocol>();

    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外<br>
     * 还需要额外保留编码和实例的映射：{别名：编码}
     */
    private final static ConcurrentMap<String, Byte>   TYPE_CODE_MAP     = new ConcurrentHashMap<String, Byte>();

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Protocol>     EXTENSION_LOADER  = buildLoader();

    private static ExtensionLoader<Protocol> buildLoader() {
        ExtensionLoader<Protocol> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Protocol.class);
        extensionLoader.addListener(new ExtensionLoaderListener<Protocol>() {
            @Override
            public void onLoad(ExtensionClass<Protocol> extensionClass) {
                // 除了保留 alias：Protocol外， 需要保留 code：Protocol
                Protocol protocol = extensionClass
                    .getExtInstance();
                TYPE_PROTOCOL_MAP.put(extensionClass.getCode(), protocol);
                TYPE_CODE_MAP.put(extensionClass.getAlias(), extensionClass.getCode());
                if (RpcConfigs.getBooleanValue(RpcOptions.TRANSPORT_SERVER_PROTOCOL_ADAPTIVE)) {
                    maxMagicOffset = 2;
                    registerAdaptiveProtocol(protocol.protocolInfo());
                }
            }
        });
        return extensionLoader;
    }

    /**
     * 按协议名称返回协议对象
     *
     * @param alias 协议名称
     * @return 协议对象
     */
    public static Protocol getProtocol(String alias) {
        // 工厂模式  托管给ExtensionLoader
        return EXTENSION_LOADER.getExtension(alias);
    }

    /**
     * 按协议编号返回协议对象
     *
     * @param code 协议编码
     * @return 协议对象
     */
    public static Protocol getProtocol(byte code) {
        Protocol protocol = TYPE_PROTOCOL_MAP.get(code);
        if (protocol == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_PROTOCOL_NOT_FOUND, code));
        }
        return protocol;
    }

    /**
     * 通过别名获取协议编码
     *
     * @param protocol 协议的名字
     * @return 协议编码
     */
    public static Byte getCodeByAlias(String protocol) {
        return TYPE_CODE_MAP.get(protocol);
    }

    /**
     * 根据头部前几个魔术位，判断是哪种协议的长连接
     *
     * @param magicHeadBytes 头部魔术位
     * @return 协议
     */
    public static Protocol adaptiveProtocol(byte[] magicHeadBytes) {
        for (Protocol protocol : TYPE_PROTOCOL_MAP.values()) {
            if (protocol.protocolInfo().isMatchMagic(magicHeadBytes)) {
                return protocol;
            }
        }
        return null;
    }

    /**
     * 最大偏移量，用于一个端口支持多协议时使用
     */
    private static int maxMagicOffset;

    /**
     * 注册协议到适配协议
     *
     * @param protocolInfo 协议描述信息
     */
    protected static synchronized void registerAdaptiveProtocol(ProtocolInfo protocolInfo) {
        // 取最大偏移量
        maxMagicOffset = Math.max(maxMagicOffset, protocolInfo.magicFieldOffset() + protocolInfo.magicFieldLength());
    }

    /**
     * 得到最大偏移位
     *
     * @return 最大偏移位
     */
    public static int getMaxMagicOffset() {
        return maxMagicOffset;
    }

}
