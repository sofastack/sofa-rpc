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
package com.alipay.sofa.rpc.doc.swagger.generate;

import io.swagger.annotations.ApiKeyAuthDefinition;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.OAuth2Definition;
import io.swagger.annotations.Scope;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.models.Contact;
import io.swagger.models.ExternalDocs;
import io.swagger.models.License;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.In;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.PathUtils;
import io.swagger.util.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The <code>Reader</code> class scans classes for Swagger annotations.
 */
public class Reader {

    private final Swagger swagger;

    private Reader(Swagger swagger) {
        this.swagger = swagger;
    }

    /**
     * Scans a set of classes for Swagger annotations.
     *
     * @param swagger is the Swagger instance
     * @param classes are a set of classes to scan
     */
    public static void read(Swagger swagger, Set<Class<?>> classes) {
        final Reader reader = new Reader(swagger);
        for (Class<?> cls : classes) {
            final ReaderContext context = new ReaderContext(swagger, cls, cls, "", null, false,
                new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                new ArrayList<Parameter>());
            reader.read(context);
        }
    }

    public static void read(Swagger swagger, Map<Class<?>, Object> interfaceMapRef,
                            String httpContext) {
        final Reader reader = new Reader(swagger);
        List<Entry<Class<?>, Object>> arrayList = new ArrayList<Entry<Class<?>, Object>>(interfaceMapRef.entrySet());
        Collections.sort(arrayList, new Comparator<Entry<Class<?>, Object>>() {
            @Override
            public int compare(Entry<Class<?>, Object> o1, Entry<Class<?>, Object> o2) {
                return o1.getKey().getSimpleName().compareTo(o2.getKey().getSimpleName());
            }
        });
        for (Entry<Class<?>, Object> entry : arrayList) {
            final ReaderContext context = new ReaderContext(swagger,
                entry.getValue().getClass(), entry.getKey(), httpContext, null, false,
                new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                new ArrayList<Parameter>());
            reader.read(context);
        }
    }

    private void read(ReaderContext context) {
        final SwaggerDefinition swaggerDefinition = context.getCls()
            .getAnnotation(SwaggerDefinition.class);
        if (swaggerDefinition != null) {
            readSwaggerConfig(swaggerDefinition);
        }
        List<Method> interfaceMethodList = Arrays.asList(context.getInterfaceCls().getMethods());
        Collections.sort(interfaceMethodList, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<Method, Method> serviceMethods = new LinkedHashMap<Method, Method>();
        for (Method method : interfaceMethodList) {
            if (!ReflectionUtils.isOverriddenMethod(method, context.getCls())) {
                serviceMethods.put(method, getRefMethod(context, method));
            }
        }

        for (Entry<Method, Method> entry : serviceMethods.entrySet()) {
            Method method = entry.getValue();
            Method interfaceMethod = entry.getKey();

            final Operation operation = new Operation();
            String operationPath = null;
            String httpMethod = null;

            ReaderExtension extension = new RpcReaderExtension();
            if (operationPath == null) {
                operationPath = extension.getPath(context, method);
            }
            if (httpMethod == null) {
                httpMethod = extension.getHttpMethod(context, method);
            }
            if (operationPath == null || httpMethod == null) {
                continue;
            }

            if (extension.isReadable(context)) {
                extension.setDeprecated(operation, method);
                extension.applyConsumes(context, operation, method);
                extension.applyProduces(context, operation, method);
                extension.applyOperationId(operation, method);
                extension.applySummary(operation, method);
                extension.applyDescription(operation, method);
                extension.applySchemes(context, operation, method);
                extension.applySecurityRequirements(context, operation, method);
                extension.applyTags(context, operation, method);
                extension.applyResponses(context, operation, method);
                extension.applyImplicitParameters(context, operation, method);
                extension.applyParameters(context, operation, method, interfaceMethod);
                extension.applyExtensions(context, operation, method);
            }

            if (httpMethod != null) {
                if (operation.getResponses() == null) {
                    operation.defaultResponse(new Response().description("successful operation"));
                }

                final Map<String, String> regexMap = new HashMap<String, String>();
                final String parsedPath = PathUtils.parsePath(operationPath, regexMap);

                Path path = swagger.getPath(parsedPath);
                if (path == null) {
                    path = new Path();
                    swagger.path(parsedPath, path);
                }
                path.set(httpMethod.toLowerCase(), operation);
            }
        }
    }

    private Method getRefMethod(ReaderContext context, Method method) {
        try {
            return context.getCls().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return method;
    }

    @SuppressWarnings("deprecation")
    private void readSwaggerConfig(SwaggerDefinition config) {
        readInfoConfig(config);

        if (StringUtils.isNotBlank(config.basePath())) {
            swagger.setBasePath(config.basePath());
        }

        if (StringUtils.isNotBlank(config.host())) {
            swagger.setHost(config.host());
        }

        for (String consume : config.consumes()) {
            if (StringUtils.isNotBlank(consume)) {
                swagger.addConsumes(consume);
            }
        }

        for (String produce : config.produces()) {
            if (StringUtils.isNotBlank(produce)) {
                swagger.addProduces(produce);
            }
        }

        if (StringUtils.isNotBlank(config.externalDocs().value())) {
            ExternalDocs externalDocs = swagger.getExternalDocs();
            if (externalDocs == null) {
                externalDocs = new ExternalDocs();
                swagger.setExternalDocs(externalDocs);
            }

            externalDocs.setDescription(config.externalDocs().value());

            if (StringUtils.isNotBlank(config.externalDocs().url())) {
                externalDocs.setUrl(config.externalDocs().url());
            }
        }
        for (OAuth2Definition oAuth2Config : config.securityDefinition().oAuth2Definitions()) {
            io.swagger.models.auth.OAuth2Definition oAuth2Definition = new io.swagger.models.auth.OAuth2Definition();
            OAuth2Definition.Flow flow = oAuth2Config.flow();

            if (flow.equals(OAuth2Definition.Flow.ACCESS_CODE)) {
                oAuth2Definition = oAuth2Definition.accessCode(oAuth2Config.authorizationUrl(),
                    oAuth2Config.tokenUrl());
            } else if (flow.equals(OAuth2Definition.Flow.APPLICATION)) {
                oAuth2Definition = oAuth2Definition.application(oAuth2Config.tokenUrl());
            } else if (flow.equals(OAuth2Definition.Flow.IMPLICIT)) {
                oAuth2Definition = oAuth2Definition.implicit(oAuth2Config.authorizationUrl());
            } else {
                oAuth2Definition = oAuth2Definition.password(oAuth2Config.tokenUrl());
            }

            for (Scope scope : oAuth2Config.scopes()) {
                oAuth2Definition.addScope(scope.name(), scope.description());
            }

            oAuth2Definition.setDescription(oAuth2Config.description());
            swagger.addSecurityDefinition(oAuth2Config.key(), oAuth2Definition);
        }

        for (ApiKeyAuthDefinition[] apiKeyAuthConfigs : new ApiKeyAuthDefinition[][] {
                config.securityDefinition().apiKeyAuthDefintions(),
                config.securityDefinition().apiKeyAuthDefinitions() }) {
            for (ApiKeyAuthDefinition apiKeyAuthConfig : apiKeyAuthConfigs) {
                io.swagger.models.auth.ApiKeyAuthDefinition apiKeyAuthDefinition = new io.swagger.models.auth.ApiKeyAuthDefinition();

                apiKeyAuthDefinition.setName(apiKeyAuthConfig.name());
                apiKeyAuthDefinition.setIn(In.forValue(apiKeyAuthConfig.in().toValue()));
                apiKeyAuthDefinition.setDescription(apiKeyAuthConfig.description());

                swagger.addSecurityDefinition(apiKeyAuthConfig.key(), apiKeyAuthDefinition);
            }
        }

        for (BasicAuthDefinition[] basicAuthConfigs : new BasicAuthDefinition[][] {
                config.securityDefinition().basicAuthDefinions(),
                config.securityDefinition().basicAuthDefinitions() }) {
            for (BasicAuthDefinition basicAuthConfig : basicAuthConfigs) {
                io.swagger.models.auth.BasicAuthDefinition basicAuthDefinition = new io.swagger.models.auth.BasicAuthDefinition();

                basicAuthDefinition.setDescription(basicAuthConfig.description());

                swagger.addSecurityDefinition(basicAuthConfig.key(), basicAuthDefinition);
            }
        }

        for (io.swagger.annotations.Tag tagConfig : config.tags()) {
            if (StringUtils.isNotBlank(tagConfig.name())) {
                final Tag tag = new Tag();
                tag.setName(tagConfig.name());
                tag.setDescription(tagConfig.description());

                if (StringUtils.isNotBlank(tagConfig.externalDocs().value())) {
                    tag.setExternalDocs(new ExternalDocs(tagConfig.externalDocs().value(),
                        tagConfig.externalDocs().url()));
                }

                tag.getVendorExtensions()
                    .putAll(BaseReaderUtils.parseExtensions(tagConfig.extensions()));

                swagger.addTag(tag);
            }
        }

        for (SwaggerDefinition.Scheme scheme : config.schemes()) {
            if (scheme != SwaggerDefinition.Scheme.DEFAULT) {
                swagger.addScheme(Scheme.forValue(scheme.name()));
            }
        }
    }

    private void readInfoConfig(SwaggerDefinition config) {
        final Info infoConfig = config.info();
        io.swagger.models.Info info = swagger.getInfo();
        if (info == null) {
            info = new io.swagger.models.Info();
            swagger.setInfo(info);
        }

        if (StringUtils.isNotBlank(infoConfig.description())) {
            info.setDescription(infoConfig.description());
        }

        if (StringUtils.isNotBlank(infoConfig.termsOfService())) {
            info.setTermsOfService(infoConfig.termsOfService());
        }

        if (StringUtils.isNotBlank(infoConfig.title())) {
            info.setTitle(infoConfig.title());
        }

        if (StringUtils.isNotBlank(infoConfig.version())) {
            info.setVersion(infoConfig.version());
        }

        if (StringUtils.isNotBlank(infoConfig.contact().name())) {
            Contact contact = info.getContact();
            if (contact == null) {
                contact = new Contact();
                info.setContact(contact);
            }

            contact.setName(infoConfig.contact().name());
            if (StringUtils.isNotBlank(infoConfig.contact().email())) {
                contact.setEmail(infoConfig.contact().email());
            }

            if (StringUtils.isNotBlank(infoConfig.contact().url())) {
                contact.setUrl(infoConfig.contact().url());
            }
        }

        if (StringUtils.isNotBlank(infoConfig.license().name())) {
            License license = info.getLicense();
            if (license == null) {
                license = new License();
                info.setLicense(license);
            }

            license.setName(infoConfig.license().name());
            if (StringUtils.isNotBlank(infoConfig.license().url())) {
                license.setUrl(infoConfig.license().url());
            }
        }

        info.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(infoConfig.extensions()));
    }

}
