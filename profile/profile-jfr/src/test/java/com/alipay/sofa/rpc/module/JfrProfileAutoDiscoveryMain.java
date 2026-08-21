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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ClientStartInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.profile.ProfileConstants;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import jdk.jfr.Recording;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 在独立JVM中验证JFR Profile可以通过正式Module SPI自动装配。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileAutoDiscoveryMain {

    private JfrProfileAutoDiscoveryMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("A recording output path is required");
        }
        Path output = Paths.get(args[0]);
        Recording recording = new Recording();
        recording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        try {
            recording.start();

            SofaRequest request = new SofaRequest();
            request.setTargetServiceUniqueName("com.example.AutoDiscoveryService:1.0");
            request.setMethodName("hello");
            request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
            ClientStartInvokeEvent startEvent = new ClientStartInvokeEvent(request);
            if (!EventBus.isEnable(ClientStartInvokeEvent.class)) {
                throw new IllegalStateException("JFR Profile module was not discovered");
            }
            EventBus.post(startEvent);
            if (request.getRequestProp(ProfileConstants.PROFILE_ID_KEY) == null) {
                throw new IllegalStateException("JFR Profile subscriber did not create a profile ID");
            }
            EventBus.post(new ClientEndInvokeEvent(request, new SofaResponse(), null));

            recording.stop();
            recording.dump(output);
        } finally {
            recording.close();
            ModuleFactory.uninstallModules();
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }
}
