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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.profile.Profile;
import com.alipay.sofa.rpc.profile.Profiles;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileSubscriberTest {

    private final JfrProfileSubscriber subscriber = new JfrProfileSubscriber();

    private final CountingProfile      profile    = new CountingProfile();

    private final SofaRequest          request    = new SofaRequest();

    private final SofaResponse         response   = new SofaResponse();

    @Before
    public void before() {
        Profiles.install(profile);
    }

    @After
    public void after() {
        Profiles.uninstall();
    }

    @Test
    public void dispatchAllRpcEvents() {
        subscriber.onEvent(new ClientStartInvokeEvent(request));
        subscriber.onEvent(new ClientBeforeSendEvent(request));
        subscriber.onEvent(new ClientAfterSendEvent(request));
        subscriber.onEvent(new ClientSyncReceiveEvent(null, null, request, response, null));
        subscriber.onEvent(new ClientAsyncReceiveEvent(null, null, request, response, null));
        subscriber.onEvent(new ClientEndInvokeEvent(request, response, null));
        subscriber.onEvent(new ServerReceiveEvent(request));
        subscriber.onEvent(new ServerSendEvent(request, response, null));
        subscriber.onEvent(new ServerEndHandleEvent());

        Assert.assertEquals(1, profile.start);
        Assert.assertEquals(1, profile.beforeSend);
        Assert.assertEquals(1, profile.afterSend);
        Assert.assertEquals(2, profile.received);
        Assert.assertEquals(1, profile.clientEnd);
        Assert.assertEquals(1, profile.serverReceived);
        Assert.assertEquals(1, profile.serverSend);
        Assert.assertEquals(2, profile.checkState);
    }

    private static class CountingProfile extends Profile {

        private int start;

        private int beforeSend;

        private int afterSend;

        private int received;

        private int clientEnd;

        private int serverReceived;

        private int serverSend;

        private int checkState;

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void startRpc(SofaRequest request) {
            start++;
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
        public void checkState() {
            checkState++;
        }
    }
}
