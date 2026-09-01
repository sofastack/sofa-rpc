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

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * Profile测试实现。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
@Extension("testProfile")
public class TestProfile extends Profile {

    public static boolean            enabled = true;
    public static boolean            fail;
    public static int                start;
    public static int                beforeSend;
    public static int                afterSend;
    public static int                received;
    public static int                clientEnd;
    public static int                serverReceived;
    public static int                serverSend;
    public static int                checkState;
    public static RpcInternalContext phaseContext;
    public static String             phase;
    public static long               phaseDuration;

    public static void reset() {
        enabled = true;
        fail = false;
        start = 0;
        beforeSend = 0;
        afterSend = 0;
        received = 0;
        clientEnd = 0;
        serverReceived = 0;
        serverSend = 0;
        checkState = 0;
        phaseContext = null;
        phase = null;
        phaseDuration = -1L;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void startRpc(SofaRequest request) {
        start++;
        if (fail) {
            throw new IllegalStateException("test profile failure");
        }
    }

    @Override
    public void clientBeforeSend(SofaRequest request) {
        beforeSend++;
    }

    @Override
    public void clientAfterSend(SofaRequest request) {
        afterSend++;
    }

    @Override
    public void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable) {
        received++;
    }

    @Override
    public void clientEnd(SofaRequest request, SofaResponse response, Throwable throwable) {
        clientEnd++;
    }

    @Override
    public void serverReceived(SofaRequest request) {
        serverReceived++;
    }

    @Override
    public void serverSend(SofaRequest request, SofaResponse response, Throwable throwable) {
        serverSend++;
    }

    @Override
    public void recordPhase(RpcInternalContext context, String phase, long elapsedNanos) {
        phaseContext = context;
        TestProfile.phase = phase;
        phaseDuration = elapsedNanos;
    }

    @Override
    public void checkState() {
        checkState++;
    }
}
