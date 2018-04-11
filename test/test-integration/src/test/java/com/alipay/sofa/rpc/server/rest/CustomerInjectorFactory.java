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

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.ValueInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import static org.jboss.resteasy.util.FindAnnotation.findAnnotation;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Provider
public class CustomerInjectorFactory extends InjectorFactoryImpl {

    @Override
    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, Class type,
                                                  Type genericType,
                                                  Annotation[] annotations, boolean useDefault,
                                                  ResteasyProviderFactory providerFactory) {
        CustomerAnnotation customerAnnotation;
        if ((customerAnnotation = findAnnotation(annotations, CustomerAnnotation.class)) != null) {
            return new CustomerInject();
        }

        return super.createParameterExtractor(injectTargetClass, injectTarget, type, genericType, annotations,
            useDefault, providerFactory);
    }
}