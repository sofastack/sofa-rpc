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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.config.TripleInterceptorManager;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.server.triple.ServerReqHeaderInterceptor;
import com.alipay.sofa.rpc.server.triple.ServerResHeaderInterceptor;
import com.alipay.sofa.rpc.transport.triple.ClientHeaderClientInterceptor;

/**
 * 该模块有两个作用：<br>
 * - 加载sofaTracer <br>
 * - 订阅事件<br>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension("sofaTracer-grpc")
public class GrpcTracerModule implements Module {

    /**
     * 是否启动Tracer
     *
     * @return 是否开启
     */
    public static boolean isEnable() {
        boolean enable = "sofaTracer".equals(RpcConfigs.getStringValue(RpcOptions.DEFAULT_TRACER));
        return enable;
    }

    @Override
    public boolean needLoad() {
        return isEnable();
    }

    @Override
    public void install() {
        // 注册Tracer相关类
        TripleInterceptorManager.registerCustomConsumerInstance(new ClientHeaderClientInterceptor());
        TripleInterceptorManager.registerCustomProviderInstance(new ServerReqHeaderInterceptor());
        TripleInterceptorManager.registerCustomProviderInstance(new ServerResHeaderInterceptor());

    }

    @Override
    public void uninstall() {
        TripleInterceptorManager.removeCustomConsumers();
        TripleInterceptorManager.removeCustomProviders();
    }
}
