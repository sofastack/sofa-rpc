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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class ProfilesTest {

    private final TestProfile profile = new TestProfile();

    @Before
    public void before() {
        TestProfile.reset();
        Profiles.install(profile);
    }

    @After
    public void after() {
        Profiles.uninstall();
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void invokeInstalledProfile() {
        Profiles.startRpc(new SofaRequest());
        Assert.assertEquals(1, TestProfile.start);

        TestProfile.enabled = false;
        Profiles.startRpc(new SofaRequest());
        Assert.assertEquals(1, TestProfile.start);
    }

    @Test
    public void isolateProfileFailure() {
        TestProfile.fail = true;
        Profiles.startRpc(new SofaRequest());
        Assert.assertEquals(1, TestProfile.start);
    }

    @Test
    public void uninstallProfile() {
        Profiles.uninstall();
        Assert.assertFalse(Profiles.isEnabled());
    }

    @Test
    public void recordPhaseWithInvocationContext() {
        RpcInternalContext context = RpcInternalContext.getContext();
        Profiles.recordPhase(context, "router", 42L);

        Assert.assertSame(context, TestProfile.phaseContext);
        Assert.assertEquals("router", TestProfile.phase);
        Assert.assertEquals(42L, TestProfile.phaseDuration);
    }
}
