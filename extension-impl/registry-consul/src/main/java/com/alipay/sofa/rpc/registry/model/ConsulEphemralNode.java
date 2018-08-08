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
package com.alipay.sofa.rpc.registry.model;

import com.alipay.sofa.rpc.registry.common.ConsulURL;
import com.alipay.sofa.rpc.registry.common.ConsulURLUtils;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;

/**
 * Consul 临时节点
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public final class ConsulEphemralNode {

    private final ConsulURL      url;

    private final String         interval;

    private final ThrallRoleType ephemralType;

    private ConsulEphemralNode(Builder builder) {
        this.url = builder.url;
        this.interval = builder.interval;
        this.ephemralType = builder.ephemralType;
    }

    public NewSession getNewSession() {
        NewSession newSersson = new NewSession();
        newSersson.setName(getSessionName());
        newSersson.setLockDelay(15);
        newSersson.setBehavior(Session.Behavior.DELETE);
        newSersson.setTtl(this.interval + "s");
        return newSersson;
    }

    public String getSessionName() {
        return ephemralType.name() + "_" + url.getHost() + "_" + url.getPort();
    }

    public String getEphemralNodeKey() {
        return ConsulURLUtils.ephemralNodePath(url, ephemralType);
    }

    public String getEphemralNodeValue() {
        return url.toFullString();
    }

    public static Builder newEphemralNode() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ephemralType == null) ? 0 : ephemralType.hashCode());
        result = prime * result + ((interval == null) ? 0 : interval.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsulEphemralNode other = (ConsulEphemralNode) obj;
        if (ephemralType != other.ephemralType)
            return false;
        if (interval == null) {
            if (other.interval != null)
                return false;
        } else if (!interval.equals(other.interval))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConsulEphemralNode [url=" + url + ", interval=" + interval + ", ephemralType=" + ephemralType + "]";
    }

    public static class Builder extends AbstractBuilder {

        private ConsulURL      url;

        private String         interval;

        private ThrallRoleType ephemralType;

        public Builder withUrl(ConsulURL url) {
            this.url = url;
            return this;
        }

        public Builder withEphemralType(ThrallRoleType ephemralType) {
            this.ephemralType = ephemralType;
            return this;
        }

        public Builder withCheckInterval(String interval) {
            this.interval = substituteEnvironmentVariables(interval);
            return this;
        }

        public ConsulEphemralNode build() {
            return new ConsulEphemralNode(this);
        }

    }

}
