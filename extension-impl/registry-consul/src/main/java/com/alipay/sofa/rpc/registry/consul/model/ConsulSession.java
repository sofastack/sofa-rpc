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
package com.alipay.sofa.rpc.registry.consul.model;

/**
 * sessions to store EphemralNode of Consul
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public final class ConsulSession {

    private String              sessionId;

    private ConsulEphemeralNode ephemralNode;

    public ConsulSession(String sessionId, ConsulEphemeralNode ephemralNode) {
        super();
        this.sessionId = sessionId;
        this.ephemralNode = ephemralNode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ConsulEphemeralNode getEphemralNode() {
        return ephemralNode;
    }

    public void setEphemralNode(ConsulEphemeralNode ephemralNode) {
        this.ephemralNode = ephemralNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ephemralNode == null) ? 0 : ephemralNode.hashCode());
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConsulSession other = (ConsulSession) obj;
        if (ephemralNode == null) {
            if (other.ephemralNode != null) {
                return false;
            }
        } else if (!ephemralNode.equals(other.ephemralNode)) {
            return false;
        }
        if (sessionId == null) {
            if (other.sessionId != null) {
                return false;
            }
        } else if (!sessionId.equals(other.sessionId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ConsulSession [sessionId=" + sessionId + ", ephemralNode=" + ephemralNode + "]";
    }

}
