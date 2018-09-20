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
import com.alipay.sofa.rpc.registry.consul.model.ConsulSession;
import com.alipay.sofa.rpc.registry.consul.model.HeartbeatService;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ttl健康检查
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class TtlScheduler {

    private static final Logger            LOGGER                   = LoggerFactory.getLogger(TtlScheduler.class);

    private final Set<HeartbeatService>    services                 = Sets.newConcurrentHashSet();

    private final Set<ConsulSession>       sessions                 = Sets.newConcurrentHashSet();

    private final Set<HeartbeatService>    failedservices           = Sets.newConcurrentHashSet();

    private final Set<ConsulSession>       failedsessions           = Sets.newConcurrentHashSet();

    private final ScheduledExecutorService heartbeatServiceExecutor = Executors.newScheduledThreadPool(1,
                                                                        new NamedThreadFactory("CheckServiceTimer",
                                                                            true));

    private final ScheduledExecutorService heartbeatSessionExecutor = Executors.newScheduledThreadPool(1,
                                                                        new NamedThreadFactory("CheckSessionTimer",
                                                                            true));

    private final ConsulClient             client;

    public TtlScheduler(ConsulClient client) {
        this.client = client;
        heartbeatServiceExecutor.scheduleAtFixedRate(new ConsulHeartbeatServiceTask(),
            ConsulConstants.HEARTBEAT_CIRCLE,
            ConsulConstants.HEARTBEAT_CIRCLE, TimeUnit.MILLISECONDS);
        heartbeatSessionExecutor.scheduleAtFixedRate(new ConsulHeartbeatSessionTask(),
            ConsulConstants.HEARTBEAT_CIRCLE,
            ConsulConstants.HEARTBEAT_CIRCLE, TimeUnit.MILLISECONDS);
    }

    public void addHeartbeatServcie(final HeartbeatService service) {
        services.add(service);
    }

    public void addHeartbeatSession(final ConsulSession session) {
        sessions.add(session);
    }

    public void removeHeartbeatServcie(final HeartbeatService service) {
        services.remove(service);
    }

    public Set<HeartbeatService> getFailedService() {
        return failedservices;
    }

    public Set<ConsulSession> getFailedSession() {
        return failedsessions;
    }

    public void cleanFailedTtl() {
        failedsessions.clear();
        failedservices.clear();
    }

    private class ConsulHeartbeatServiceTask implements Runnable {

        @Override
        public void run() {
            for (HeartbeatService service : services) {
                try {
                    String checkId = service.getNewService().getId();
                    if (!checkId.startsWith("service:")) {
                        checkId = "service:" + checkId;
                    }
                    client.agentCheckPass(checkId);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Sending consul heartbeat for: {}", checkId);
                    }
                } catch (Throwable e) {
                    failedservices.add(service);
                    services.remove(service);
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    private class ConsulHeartbeatSessionTask implements Runnable {

        @Override
        public void run() {
            Set<String> sessionIds = Sets.newHashSet();
            for (ConsulSession session : sessions) {
                try {
                    String sessionId = session.getSessionId();
                    if (!sessionIds.contains(sessionId)) {
                        client.renewSession(sessionId, QueryParams.DEFAULT);
                        sessionIds.add(sessionId);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Sending consul heartbeat for: {}", sessionId);
                    }
                } catch (Throwable e) {
                    failedsessions.addAll(sessions);
                    sessions.clear();
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
