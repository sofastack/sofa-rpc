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
package com.alipay.sofa.rpc.codec;

import com.alipay.sofa.rpc.common.struct.TwoWayMap;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoader;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderListener;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 序列化工厂
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class SerializerFactory {

    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外<br>
     * 还需要额外保留编码和实例的映射：{编码：序列化器}
     */
    private final static ConcurrentMap<Byte, Serializer> TYPE_SERIALIZER_MAP = new ConcurrentHashMap<Byte, Serializer>();

    /**
     * 除了托管给扩展加载器的工厂模式（保留alias：实例）外，还需要额外保留编码和实例的映射：{别名：编码}
     */
    private final static TwoWayMap<String, Byte>         TYPE_CODE_MAP       = new TwoWayMap<String, Byte>();

    /**
     * 扩展加载器
     */
    private final static ExtensionLoader<Serializer>     EXTENSION_LOADER    = buildLoader();

    private static ExtensionLoader<Serializer> buildLoader() {
        ExtensionLoader<Serializer> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Serializer.class);
        extensionLoader.addListener(new ExtensionLoaderListener<Serializer>() {
            @Override
            public void onLoad(ExtensionClass<Serializer> extensionClass) {
                // 除了保留 tag：Serializer外， 需要保留 code：Serializer
                TYPE_SERIALIZER_MAP.put(extensionClass.getCode(), extensionClass.getExtInstance());
                TYPE_CODE_MAP.put(extensionClass.getAlias(), extensionClass.getCode());
            }
        });
        return extensionLoader;

    }

    /**
     * 按序列化名称返回协议对象
     *
     * @param alias 序列化名称
     * @return 序列化器
     */
    public static Serializer getSerializer(String alias) {
        // 工厂模式  托管给ExtensionLoader
        return EXTENSION_LOADER.getExtension(alias);
    }

    /**
     * 按序列化名称返回协议对象
     *
     * @param type 序列号编码
     * @return 序列化器
     */
    public static Serializer getSerializer(byte type) {
        Serializer serializer = TYPE_SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SERIALIZER_NOT_FOUND, type));
        }
        return serializer;
    }

    /**
     * 通过别名获取Code
     *
     * @param serializer 序列化的名字
     * @return 序列化编码
     */
    public static Byte getCodeByAlias(String serializer) {
        return TYPE_CODE_MAP.get(serializer);
    }

    /**
     * 通过Code获取别名
     *
     * @param code 序列化的Code
     * @return 序列化别名
     */
    public static String getAliasByCode(byte code) {
        return TYPE_CODE_MAP.getKey(code);
    }

}
