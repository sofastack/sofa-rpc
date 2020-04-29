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

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceLocatorInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.spi.metadata.ResourceMethod;

/**
 * SofaResourceMethodRegistry base on ResourceMethodRegistry.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see ResourceMethodRegistry
 */
public class SofaResourceMethodRegistry extends ResourceMethodRegistry {
    public SofaResourceMethodRegistry(ResteasyProviderFactory providerFactory) {
        super(providerFactory);
    }

    /**
     * Add a custom resource implementation endpoint.
     *
     * @param ref
     * @param basePath prefix path of resource
     */
    @Override
    public void addResourceFactory(ResourceFactory ref, String basePath) {
        // 多应用下改造点
        super.addResourceFactory(ref, basePath);
    }

    @Override
    public ResourceInvoker getResourceInvoker(HttpRequest request) {
        // 多应用下改造点
        return super.getResourceInvoker(request);
    }

    @Override
    protected void processMethod(ResourceFactory rf, String base, ResourceLocator method)
    {
        ResteasyUriBuilder builder = new ResteasyUriBuilder();
        if (base != null) {
            builder.path(base);
        }
        builder.path(method.getFullpath());
        String fullpath = builder.getPath();
        if (fullpath == null) {
            fullpath = "";
        }

        builder = new ResteasyUriBuilder();
        if (base != null) {
            builder.path(base);
        }
        builder.path(method.getResourceClass().getPath());
        String classExpression = builder.getPath();
        if (classExpression == null) {
            classExpression = "";
        }

        InjectorFactory injectorFactory = providerFactory.getInjectorFactory();
        if (method instanceof ResourceMethod)
        {
            ResourceMethodInvoker invoker = new SofaResourceMethodInvoker((ResourceMethod) method, injectorFactory, rf,
                providerFactory); // CHANGE
            if (widerMatching) {
                rootNode.addInvoker(fullpath, invoker);
            } else {
                root.addInvoker(classExpression, fullpath, invoker);
            }
        }
        else
        {
            ResourceLocatorInvoker locator = new ResourceLocatorInvoker(rf, injectorFactory, providerFactory, method);
            if (widerMatching) {
                rootNode.addInvoker(fullpath, locator);
            } else {
                root.addInvoker(classExpression, fullpath, locator);
            }
        }
    }
}
