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

import java.util.List;

public final class ConsulServiceResp {

    private final List<ConsulService> consulServices;
    private final Long                consulIndex;
    private final Boolean             consulKnownLeader;
    private final Long                consulLastContact;

    private ConsulServiceResp(Builder builder) {
        this.consulServices = builder.consulServices;
        this.consulIndex = builder.consulIndex;
        this.consulKnownLeader = builder.consulKnownLeader;
        this.consulLastContact = builder.consulLastContact;
    }

    public List<ConsulService> getConsulServices() {
        return consulServices;
    }

    public Long getConsulIndex() {
        return consulIndex;
    }

    public Boolean getConsulKnownLeader() {
        return consulKnownLeader;
    }

    public Long getConsulLastContact() {
        return consulLastContact;
    }

    public static Builder newResponse() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder {

        private List<ConsulService> consulServices;
        private Long                consulIndex;
        private Boolean             consulKnownLeader;
        private Long                consulLastContact;

        public Builder withValue(List<ConsulService> value) {
            this.consulServices = value;
            return this;
        }

        public Builder withConsulIndex(Long consulIndex) {
            this.consulIndex = consulIndex;
            return this;
        }

        public Builder withConsulKnowLeader(Boolean consulKnownLeader) {
            this.consulKnownLeader = consulKnownLeader;
            return this;
        }

        public Builder withConsulLastContact(Long consulLastContact) {
            this.consulLastContact = consulLastContact;
            return this;
        }

        public ConsulServiceResp build() {
            return new ConsulServiceResp(this);
        }

    }
}
