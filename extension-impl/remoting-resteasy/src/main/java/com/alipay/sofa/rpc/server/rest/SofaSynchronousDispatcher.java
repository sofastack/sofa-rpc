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
package com.alipay.sofa.rpc.server.rest;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InternalDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.ext.Providers;

/**
 * SofaSynchronousDispatcher base on SynchronousDispatcher
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaSynchronousDispatcher extends SynchronousDispatcher {
    public SofaSynchronousDispatcher(ResteasyProviderFactory providerFactory) {
        super(providerFactory);
        this.providerFactory = providerFactory;
        this.registry = new SofaResourceMethodRegistry(providerFactory); // CHANGE
        defaultContextObjects.put(Providers.class, providerFactory);
        defaultContextObjects.put(Registry.class, registry);
        defaultContextObjects.put(Dispatcher.class, this);
        defaultContextObjects.put(InternalDispatcher.class, InternalDispatcher.getInstance());
    }

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ResourceInvoker invoker) {
        // 可以执行一下逻辑，例如切换ClassLoader等
        super.invoke(request, response, invoker);
    }
}
