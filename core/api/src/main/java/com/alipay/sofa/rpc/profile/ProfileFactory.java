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
package com.alipay.sofa.rpc.profile;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * Profile扩展工厂。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
public final class ProfileFactory {

    private ProfileFactory() {
    }

    /**
     * 根据扩展别名获取Profile实例。
     *
     * @param profileName 扩展别名
     * @return Profile实例
     */
    public static synchronized Profile getProfile(String profileName) {
        try {
            ExtensionClass<Profile> ext = ExtensionLoaderFactory.getExtensionLoader(Profile.class)
                .getExtensionClass(profileName);
            if (ext == null) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT,
                    Profile.class.getName(), profileName));
            }
            return ext.getExtInstance();
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT,
                Profile.class.getName(), profileName), e);
        }
    }
}
