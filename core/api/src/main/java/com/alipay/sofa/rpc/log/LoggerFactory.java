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
package com.alipay.sofa.rpc.log;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

/**
 * Factory of logger.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class LoggerFactory {

    /**
     * 配置的实现类
     */
    private static String implClass = RpcConfigs.getStringValue(RpcOptions.LOGGER_IMPL);

    public static Logger getLogger(String name) {
        try {
            Object logInstance = ClassUtils.forName(implClass, Logger.class.getClassLoader())
                .getConstructor(String.class)
                .newInstance(name);
            if (logInstance instanceof Logger) {
                return (Logger) logInstance;
            } else {
                throw new SofaRpcRuntimeException(implClass + " is not type of  " + Logger.class);
            }
        } catch (SofaRpcRuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Error when getLogger of " + name
                + ", implement is " + implClass + "", e);
        }
    }

    public static Logger getLogger(Class clazz) {
        try {
            Object logInstance = ClassUtils.forName(implClass, Logger.class.getClassLoader())
                .getConstructor(Class.class).newInstance(clazz);
            if (logInstance instanceof Logger) {
                return (Logger) logInstance;
            } else {
                throw new SofaRpcRuntimeException(implClass + " is not type of  " + Logger.class);
            }
        } catch (SofaRpcRuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Error when getLogger of " + clazz.getName()
                + ", implement is " + implClass + "", e);
        }
    }
}
