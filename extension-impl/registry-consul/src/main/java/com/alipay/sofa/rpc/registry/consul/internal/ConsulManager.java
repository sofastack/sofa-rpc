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
package com.alipay.sofa.rpc.registry.consul.internal;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.registry.consul.common.ConsulConstants;
import com.alipay.sofa.rpc.registry.consul.model.ConsulEphemeralNode;
import com.alipay.sofa.rpc.registry.consul.model.ConsulRouterResp;
import com.alipay.sofa.rpc.registry.consul.model.ConsulService;
import com.alipay.sofa.rpc.registry.consul.model.ConsulServiceResp;
import com.alipay.sofa.rpc.registry.consul.model.ConsulSession;
import com.alipay.sofa.rpc.registry.consul.model.HeartbeatService;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 封装ecwid consul client
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class ConsulManager {

    private static final Logger            LOGGER = LoggerFactory.getLogger(ConsulManager.class);

    private final Object                   lock   = new Object();

    private final ConsulClient             client;

    private final TtlScheduler             ttlScheduler;

    private final ScheduledExecutorService scheduleRegistry;

    public ConsulManager(String host, int port) {
        client = new ConsulClient(host, port);
        ttlScheduler = new TtlScheduler(client);
        scheduleRegistry = Executors.newScheduledThreadPool(1, new NamedThreadFactory("retryFailedTtl", true));
        scheduleRegistry.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    retryFailedTtl();
                } catch (Throwable e) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("retry registry znode failed", e);
                    }
                }
            }
        }, ConsulConstants.HEARTBEAT_CIRCLE, ConsulConstants.HEARTBEAT_CIRCLE, TimeUnit.MILLISECONDS);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("ConsulEcwidClient init finish. client host:" + host + ", port:" + port);
        }
    }

    private void retryFailedTtl() {
        Set<HeartbeatService> failedService = ttlScheduler.getFailedService();
        Set<ConsulSession> failedSession = ttlScheduler.getFailedSession();
        if (failedSession.size() > 0 || failedService.size() > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("retry to registry failed service %d or failed session %d",
                    failedService.size(),
                    failedSession.size()));
            }
            for (HeartbeatService heartbeatService : failedService) {
                registerService(heartbeatService.getService());
            }
            Set<Boolean> allSuccess = Sets.newHashSet();
            for (ConsulSession consulSession : failedSession) {
                allSuccess.add(registerEphemralNode(consulSession.getEphemralNode()));
            }
            if (!allSuccess.contains(Boolean.FALSE)) {
                ttlScheduler.cleanFailedTtl();
            }
        }
    }

    public void registerService(ConsulService service) {
        NewService newService = service.getNewService();
        client.agentServiceRegister(newService);
        HeartbeatService heartbeatService = new HeartbeatService(service, newService);
        ttlScheduler.addHeartbeatServcie(heartbeatService);
    }

    public void unregisterService(ConsulService service) {
        NewService newService = service.getNewService();
        client.agentServiceDeregister(newService.getId());
        HeartbeatService heartbeatService = new HeartbeatService(service, newService);
        ttlScheduler.removeHeartbeatServcie(heartbeatService);
    }

    public Boolean registerEphemralNode(ConsulEphemeralNode ephemralNode) {
        String sessionId = null;
        List<Session> sessions = client.getSessionList(QueryParams.DEFAULT).getValue();
        if (sessions != null && !sessions.isEmpty()) {
            for (Session session : sessions) {
                if (session.getName().equals(ephemralNode.getSessionName())) {
                    sessionId = session.getId();
                }
            }
        }
        if (sessionId == null) {
            NewSession newSession = ephemralNode.getNewSession();
            synchronized (lock) {
                sessionId = client.sessionCreate(newSession, QueryParams.DEFAULT).getValue();
            }
        }
        ConsulSession session = new ConsulSession(sessionId, ephemralNode);
        ttlScheduler.addHeartbeatSession(session);
        PutParams kvPutParams = new PutParams();
        kvPutParams.setAcquireSession(sessionId);

        client.getKVValue(ephemralNode.getEphemralNodeKey());

        return client.setKVValue(ephemralNode.getEphemralNodeKey(), ephemralNode.getEphemralNodeValue(),
            kvPutParams).getValue();
    }

    public ConsulRouterResp lookupRouterMessage(String serviceName, long lastConsulIndex) {
        QueryParams queryParams = new QueryParams(ConsulConstants.CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<GetValue> orgResponse = client.getKVValue(serviceName, queryParams);
        GetValue getValue = orgResponse.getValue();
        if (getValue != null && StringUtils.isNotBlank(getValue.getValue())) {
            String router = new String(Base64.decodeBase64(getValue.getValue()));
            ConsulRouterResp response = ConsulRouterResp.newResponse()//
                .withValue(router)//
                .withConsulIndex(orgResponse.getConsulIndex())//
                .withConsulLastContact(orgResponse.getConsulLastContact())//
                .withConsulKnowLeader(orgResponse.isConsulKnownLeader())//
                .build();
            return response;
        }
        return null;
    }

    public ConsulServiceResp lookupHealthService(String serviceName, long lastConsulIndex) {
        QueryParams queryParams = new QueryParams(ConsulConstants.CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> orgResponse = client.getHealthServices(serviceName, true, queryParams);
        if (orgResponse != null && orgResponse.getValue() != null && !orgResponse.getValue().isEmpty()) {
            List<HealthService> healthServices = orgResponse.getValue();
            List<ConsulService> consulServices = Lists.newArrayList();
            for (HealthService orgService : healthServices) {
                Service org = orgService.getService();
                ConsulService newService = ConsulService.newService()//
                    .withAddress(org.getAddress())//
                    .withName(org.getService())//
                    .withId(org.getId())//
                    .withPort(org.getPort().toString())//
                    .withTags(org.getTags())//
                    .build();
                consulServices.add(newService);
            }
            if (!consulServices.isEmpty()) {
                ConsulServiceResp response = ConsulServiceResp.newResponse()//
                    .withValue(consulServices)//
                    .withConsulIndex(orgResponse.getConsulIndex())//
                    .withConsulLastContact(orgResponse.getConsulLastContact())//
                    .withConsulKnowLeader(orgResponse.isConsulKnownLeader())//
                    .build();
                return response;
            }
        }
        return null;
    }

}
