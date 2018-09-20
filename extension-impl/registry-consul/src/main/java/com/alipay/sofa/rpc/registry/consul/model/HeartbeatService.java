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

import com.ecwid.consul.v1.agent.model.NewService;

/**
 * ConsulService and NewService
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class HeartbeatService {

    private ConsulService service;

    private NewService    newService;

    public HeartbeatService(ConsulService service, NewService newService) {
        super();
        this.service = service;
        this.newService = newService;
    }

    public ConsulService getService() {
        return service;
    }

    public void setService(ConsulService service) {
        this.service = service;
    }

    public NewService getNewService() {
        return newService;
    }

    public void setNewService(NewService newService) {
        this.newService = newService;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((newService == null) ? 0 : newService.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
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
        HeartbeatService other = (HeartbeatService) obj;
        if (newService == null) {
            if (other.newService != null) {
                return false;
            }
        } else if (!newService.equals(other.newService)) {
            return false;
        }
        if (service == null) {
            if (other.service != null) {
                return false;
            }
        } else if (!service.equals(other.service)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HeartbeatService [service=" + service + ", newService=" + newService + "]";
    }

}
