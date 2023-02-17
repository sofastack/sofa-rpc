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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.alipay.sofa.rpc.common.RpcConstants.CUSTOM_CALLER_APP;
import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_APP_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

/**
 * @author zhaowang
 * @version : DefaultClientProxyInvokerTest.java, v 0.1 2022年06月13日 3:52 下午 zhaowang
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultClientProxyInvokerTest {

    private String                    originCallerApp = "originCallerApp";
    private String                    customCallerApp = "customCallerApp";
    @Mock
    private DefaultClientProxyInvoker invoker;

    @Before
    public void before() {
        doCallRealMethod().when(invoker).customRequest(any(), any());
    }

    @Test
    public void testCustomCtx() {
        RpcInternalContext internalContext = RpcInternalContext.getContext();
        SofaRequest request = getRequest();
        invoker.customRequest(request, internalContext);
        Assert.assertEquals(originCallerApp, request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Assert.assertEquals(originCallerApp, internalContext.getAttachment(INTERNAL_KEY_APP_NAME));

        request = getRequest();
        RpcInvokeContext.getContext().put(CUSTOM_CALLER_APP, customCallerApp);
        invoker.customRequest(request, internalContext);
        Assert.assertEquals(customCallerApp, request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Assert.assertEquals(customCallerApp, internalContext.getAttachment(INTERNAL_KEY_APP_NAME));

        request = getRequest();
        RpcInvokeContext.getContext().put(CUSTOM_CALLER_APP, new Object());
        invoker.customRequest(request, internalContext);
        Assert.assertEquals(originCallerApp, request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Assert.assertEquals(originCallerApp, internalContext.getAttachment(INTERNAL_KEY_APP_NAME));

        request = getRequest();
        RpcInvokeContext.getContext().put(CUSTOM_CALLER_APP, "");
        invoker.customRequest(request, internalContext);
        Assert.assertEquals(originCallerApp, request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Assert.assertEquals(originCallerApp, internalContext.getAttachment(INTERNAL_KEY_APP_NAME));

        request = getRequest();
        RpcInvokeContext.getContext().put(CUSTOM_CALLER_APP, null);
        invoker.customRequest(request, internalContext);
        Assert.assertEquals(originCallerApp, request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Assert.assertEquals(originCallerApp, internalContext.getAttachment(INTERNAL_KEY_APP_NAME));
    }

    private SofaRequest getRequest() {
        SofaRequest sofaRequest = new SofaRequest();
        sofaRequest.addRequestProp(RemotingConstants.HEAD_APP_NAME, originCallerApp);
        RpcInternalContext.getContext().setAttachment(INTERNAL_KEY_APP_NAME, originCallerApp);
        return sofaRequest;
    }
}