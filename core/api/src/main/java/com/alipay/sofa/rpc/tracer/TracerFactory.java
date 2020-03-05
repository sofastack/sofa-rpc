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
package com.alipay.sofa.rpc.tracer;

import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * Factory of Tracer
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class TracerFactory {

    /**
     * 初始化Tracer实例
     *
     * @return Tracer
     */
    public synchronized static Tracer getTracer(String tracerName) {
        try {
            ExtensionClass<Tracer> ext = ExtensionLoaderFactory.getExtensionLoader(Tracer.class)
                .getExtensionClass(tracerName);
            if (ext == null) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_FAIL_LOAD_TRACER_EXT, tracerName));
            }
            return ext.getExtInstance();
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_FAIL_LOAD_TRACER_EXT, tracerName), e);
        }
    }
}
