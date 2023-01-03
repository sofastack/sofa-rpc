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
package com.alipay.sofa.rpc.codec.sofahessian.serialize;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;

/**
 *
 * @author xingqi
 * @version : CustomThrowableGenericDeserialize.java, v 0.1 2022年10月18日 5:02 PM xingqi Exp $
 */
public class GenericCustomThrowableDeterminer {

    private static final boolean  GENERIC_THROW_EXCEPTION = SofaConfigs
                                                              .getOrDefault(RpcConfigKeys.GENERIC_THROW_EXCEPTION);

    private static final String[] THROWABLE_FIELDS        = SofaConfigs
                                                              .getOrDefault(RpcConfigKeys.GENERIC_THROWABLE_FIELDS);

    public static Object judgeCustomThrowableForGenericObject(Object appObject) {
        if (!GENERIC_THROW_EXCEPTION || appObject == null) {
            return appObject;
        }
        if (!(appObject instanceof GenericObject)) {
            return appObject;
        }
        for (String field : THROWABLE_FIELDS) {
            if (!((GenericObject) appObject).hasField(field)) {
                return appObject;
            }
        }
        return new RuntimeException(
            "occur business exception, but type=" + ((GenericObject) appObject).getType() +
                " class is not found, error: " + appObject);
    }

    public static boolean isGenericThrowException() {
        return GENERIC_THROW_EXCEPTION;
    }
}