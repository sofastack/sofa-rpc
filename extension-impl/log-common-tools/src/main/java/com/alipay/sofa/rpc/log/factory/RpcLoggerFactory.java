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
package com.alipay.sofa.rpc.log.factory;

import com.alipay.sofa.common.log.LoggerSpaceManager;
import com.alipay.sofa.common.log.SpaceId;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 rpc 的打印日志工厂
 * 
 * @author <a href=mailto:guanchao.ygc@antfin.com>Guanchao Yang</a>
 */
public class RpcLoggerFactory {

    public static final String  RPC_LOG_SPACE = "com.alipay.sofa.rpc";

    private static final String APPNAME       = "appname";

    /**
     * 获取日志对象
     *
     * @param name 日志的名字
     * @return 日志实现
     */
    public static org.slf4j.Logger getLogger(String name, String appname) {
        //从"com/alipay/sofa/rpc/log"中获取 rpc 的日志配置并寻找对应logger对象,log 为默认添加的后缀
        if (name == null || name.isEmpty()) {
            return null;
        }

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(APPNAME, appname == null ? "" : appname);
        SpaceId spaceId = new SpaceId(RPC_LOG_SPACE);
        if (appname != null) {
            spaceId.withTag(APPNAME, appname);
        }
        return LoggerSpaceManager.getLoggerBySpace(name, spaceId, properties);
    }
}
