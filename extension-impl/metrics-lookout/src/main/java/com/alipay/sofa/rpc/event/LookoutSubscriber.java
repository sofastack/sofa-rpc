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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.metrics.lookout.RpcClientLookoutModel;
import com.alipay.sofa.rpc.metrics.lookout.RpcLookout;
import com.alipay.sofa.rpc.metrics.lookout.RpcServerLookoutModel;

/**
 * Collect the raw information for lookout by listening to events.
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class LookoutSubscriber extends Subscriber {

    /**
     * Whether lookout be banned from collecting information.
     */
    public static boolean    lookoutCollectDisable = RpcConfigs
                                                       .getBooleanValue(RpcOptions.LOOKOUT_COLLECT_DISABLE);

    private final RpcLookout rpcMetrics            = new RpcLookout();

    public LookoutSubscriber() {
        super(false);
    }

    @Override
    public void onEvent(Event event) {

        if (RpcRunningState.isUnitTestMode() || lookoutCollectDisable) {
            return;
        }

        Class eventClass = event.getClass();

        if (eventClass == ClientEndInvokeEvent.class) {

            ClientEndInvokeEvent clientEndInvokeEvent = (ClientEndInvokeEvent) event;

            RpcClientLookoutModel rpcClientMetricsModel = createClientMetricsModel(clientEndInvokeEvent.getRequest(),
                clientEndInvokeEvent.getResponse());

            rpcMetrics.collectClient(rpcClientMetricsModel);

        } else if (eventClass == ServerSendEvent.class) {

            ServerSendEvent serverSendEvent = (ServerSendEvent) event;

            RpcServerLookoutModel rpcServerMetricsModel = createServerMetricsModel(serverSendEvent.getRequest(),
                serverSendEvent.getResponse());

            rpcMetrics.collectServer(rpcServerMetricsModel);

        } else if (eventClass == ServerStartedEvent.class) {

            ServerStartedEvent serverStartedEvent = (ServerStartedEvent) event;

            rpcMetrics.collectThreadPool(serverStartedEvent.getServerConfig(),
                serverStartedEvent.getThreadPoolExecutor());

        } else if (eventClass == ServerStoppedEvent.class) {
            ServerStoppedEvent serverStartedEvent = (ServerStoppedEvent) event;

            rpcMetrics.removeThreadPool(serverStartedEvent.getServerConfig());
        } else if (eventClass == ProviderPubEvent.class) {
            ProviderPubEvent providerPubEvent = (ProviderPubEvent) event;
            rpcMetrics.collectProvderPubInfo(providerPubEvent.getProviderConfig());
        }
        else if (eventClass == ConsumerSubEvent.class) {
            ConsumerSubEvent consumerSubEvent = (ConsumerSubEvent) event;
            rpcMetrics.collectConsumerSubInfo(consumerSubEvent.getConsumerConfig());
        }
    }

    /**
     * create RpcClientLookoutModel
     * @param request
     * @param response
     * @return
     */
    private RpcClientLookoutModel createClientMetricsModel(SofaRequest request, SofaResponse response) {

        RpcClientLookoutModel clientMetricsModel = new RpcClientLookoutModel();

        RpcInternalContext context = RpcInternalContext.getContext();

        String app = getStringAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_APP_NAME));
        String service = request.getTargetServiceUniqueName();
        String method = request.getMethodName();
        String protocol = getStringAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_PROTOCOL_NAME));
        String invokeType = request.getInvokeType();
        String targetApp = request.getTargetAppName();
        Long requestSize = getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE));
        Long responseSize = getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE));
        Long elapsedTime = getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE));
        Boolean success = response != null && !response.isError() && response.getErrorMsg() == null &&
            (!(response.getAppResponse() instanceof Throwable));

        clientMetricsModel.setApp(app);
        clientMetricsModel.setService(service);
        clientMetricsModel.setMethod(method);
        clientMetricsModel.setProtocol(protocol);
        clientMetricsModel.setInvokeType(invokeType);
        clientMetricsModel.setTargetApp(targetApp);
        clientMetricsModel.setRequestSize(requestSize);
        clientMetricsModel.setResponseSize(responseSize);
        clientMetricsModel.setElapsedTime(elapsedTime);
        clientMetricsModel.setSuccess(success);

        return clientMetricsModel;
    }

    /**
     * create RpcServerLookoutModel
     * @param request
     * @param response
     * @return
     */
    private RpcServerLookoutModel createServerMetricsModel(SofaRequest request, SofaResponse response) {

        RpcServerLookoutModel rpcServerMetricsModel = new RpcServerLookoutModel();

        RpcInternalContext context = RpcInternalContext.getContext();

        String app = request.getTargetAppName();
        String service = request.getTargetServiceUniqueName();
        String method = request.getMethodName();
        String protocol = getStringAvoidNull(request.getRequestProp(RemotingConstants.HEAD_PROTOCOL));
        String invokeType = request.getInvokeType();
        String callerApp = getStringAvoidNull(request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        Long elapsedTime = getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE));
        boolean success = response != null && !response.isError() && response.getErrorMsg() == null &&
            (!(response.getAppResponse() instanceof Throwable));

        rpcServerMetricsModel.setApp(app);
        rpcServerMetricsModel.setService(service);
        rpcServerMetricsModel.setMethod(method);
        rpcServerMetricsModel.setProtocol(protocol);
        rpcServerMetricsModel.setInvokeType(invokeType);
        rpcServerMetricsModel.setCallerApp(callerApp);
        rpcServerMetricsModel.setElapsedTime(elapsedTime);
        rpcServerMetricsModel.setSuccess(success);

        return rpcServerMetricsModel;
    }

    private String getStringAvoidNull(Object object) {
        if (object == null) {
            return null;
        }

        return (String) object;

    }

    private Long getLongAvoidNull(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Integer) {
            return Long.parseLong(object.toString());
        }

        return (Long) object;
    }

    public static boolean isLookoutCollectDisable() {
        return lookoutCollectDisable;
    }

    public static void setLookoutCollectDisable(boolean lookoutCollectDisable) {
        LookoutSubscriber.lookoutCollectDisable = lookoutCollectDisable;
    }
}