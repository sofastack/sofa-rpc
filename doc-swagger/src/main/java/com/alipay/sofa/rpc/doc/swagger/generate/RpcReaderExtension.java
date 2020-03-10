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

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.doc.swagger.utils.LocalVariableTableParameterNameDiscoverer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import io.swagger.converter.ModelConverters;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.ParameterProcessor;
import io.swagger.util.PathUtils;
import io.swagger.util.PrimitiveType;
import io.swagger.util.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RpcReaderExtension implements ReaderExtension {

    private final LocalVariableTableParameterNameDiscoverer localVariableTableParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isReadable(ReaderContext context) {
        final Api apiAnnotation = context.getCls().getAnnotation(Api.class);
        // read do not have Api annotation cls, skip hidden cls
        return apiAnnotation == null || (context.isReadHidden() || !apiAnnotation.hidden());
    }

    @Override
    public void applyConsumes(ReaderContext context, Operation operation, Method method) {
        final List<String> consumes = new ArrayList<String>();
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);

        if (apiOperation != null) {
            consumes.addAll(parseStringValues(apiOperation.consumes()));
        }

        if (consumes.isEmpty()) {
            final Api apiAnnotation = context.getCls().getAnnotation(Api.class);
            if (apiAnnotation != null) {
                consumes.addAll(parseStringValues(apiAnnotation.consumes()));
            }
            consumes.addAll(context.getParentConsumes());
        }

        for (String consume : consumes) {
            operation.consumes(consume);
        }

    }

    private static List<String> parseStringValues(String str) {
        return parseAnnotationValues(str, new Function<String, String>() {
            @Override
            public String apply(String value) {
                return value;
            }
        });
    }

    private static <T> List<T> parseAnnotationValues(String str, Function<String, T> processor) {
        final List<T> result = new ArrayList<T>();
        for (String item : Splitter.on(",").trimResults().omitEmptyStrings().split(str)) {
            result.add(processor.apply(item));
        }
        return result;
    }

    @Override
    public void applyProduces(ReaderContext context, Operation operation, Method method) {
        final List<String> produces = new ArrayList<String>();
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);

        if (apiOperation != null) {
            produces.addAll(parseStringValues(apiOperation.produces()));
        }

        if (produces.isEmpty()) {
            final Api apiAnnotation = context.getCls().getAnnotation(Api.class);
            if (apiAnnotation != null) {
                produces.addAll(parseStringValues(apiAnnotation.produces()));
            }
            produces.addAll(context.getParentProduces());
        }

        for (String produce : produces) {
            operation.produces(produce);
        }

    }

    @Override
    public String getHttpMethod(ReaderContext context, Method method) {
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        return apiOperation == null || StringUtils.isEmpty(apiOperation.httpMethod())
            ? HttpMethod.POST.name() : apiOperation.httpMethod();
    }

    @Override
    public String getPath(ReaderContext context, Method method) {
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        String operationId = null == apiOperation ? ""
            : StringUtils.isBlank(apiOperation.nickname()) ? null : apiOperation.nickname();
        return PathUtils.collectPath(context.getParentPath(), context.getInterfaceCls().getName(),
            method.getName(), operationId);

    }

    @Override
    public void applyOperationId(Operation operation, Method method) {
        operation.operationId(method.getName());

    }

    @Override
    public void applySummary(Operation operation, Method method) {
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null && StringUtils.isNotBlank(apiOperation.value())) {
            operation.summary(apiOperation.value());
        }

    }

    @Override
    public void applyDescription(Operation operation, Method method) {
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null && StringUtils.isNotBlank(apiOperation.notes())) {
            operation.description(apiOperation.notes());
        }
        operation.description(operation.getDescription() == null ? outputMethod(method)
            : (outputMethod(method) + operation.getDescription()));
    }

    private String outputMethod(Method method) {
        try {
            Class<?>[] types = method.getParameterTypes();
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            StringBuilder sb = new StringBuilder();

            sb.append(method.getReturnType().getSimpleName()).append(" ").append(method.getName());

            sb.append('(');
            for (int j = 0; j < types.length; j++) {
                sb.append(types[j].getName());
                if (j < (types.length - 1)) {
                    sb.append(",");
                }
            }
            sb.append(')');
            if (exceptionTypes.length > 0) {
                sb.append(" throws ");
                for (int j = 0; j < exceptionTypes.length; j++) {
                    sb.append(exceptionTypes[j].getSimpleName());
                    if (j < (exceptionTypes.length - 1)) {
                        sb.append(",");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }

    }

    @Override
    public void applySchemes(ReaderContext context, Operation operation, Method method) {
        final List<Scheme> schemes = new ArrayList<Scheme>();
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        final Api apiAnnotation = context.getCls().getAnnotation(Api.class);

        if (apiOperation != null) {
            schemes.addAll(parseSchemes(apiOperation.protocols()));
        }

        if (schemes.isEmpty() && apiAnnotation != null) {
            schemes.addAll(parseSchemes(apiAnnotation.protocols()));
        }

        for (Scheme scheme : schemes) {
            operation.scheme(scheme);
        }

    }

    private static List<Scheme> parseSchemes(String schemes) {
        final List<Scheme> result = new ArrayList<Scheme>();
        for (String item : StringUtils.trimToEmpty(schemes).split(",")) {
            final Scheme scheme = Scheme.forValue(StringUtils.trimToNull(item));
            if (scheme != null && !result.contains(scheme)) {
                result.add(scheme);
            }
        }
        return result;
    }

    @Override
    public void setDeprecated(Operation operation, Method method) {
        operation.deprecated(ReflectionUtils.getAnnotation(method, Deprecated.class) != null);

    }

    @Override
    public void applySecurityRequirements(ReaderContext context, Operation operation,
                                          Method method) {
    }

    // @Override
    // public void applyTags(ReaderContext context) {
    // final Api apiAnnotation = context.getCls().getAnnotation(Api.class);
    // if (apiAnnotation != null) {
    // Tag tag = new
    // Tag().name(context.getInterfaceCls().getSimpleName()).description(apiAnnotation.value());
    // }
    // }

    @Override
    public void applyTags(ReaderContext context, Operation operation, Method method) {
        final List<String> tags = new ArrayList<String>();

        final Api apiAnnotation = context.getCls().getAnnotation(Api.class);
        if (apiAnnotation != null) {
            Collection<String> filter = Collections2.filter(Arrays.asList(apiAnnotation.tags()),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        return StringUtils.isNotBlank(input);
                    }
                });
            if (filter.isEmpty()) {
                tags.add(context.getInterfaceCls().getSimpleName());
            } else {
                tags.addAll(filter);
            }
        } else {
            tags.add(context.getInterfaceCls().getSimpleName());
        }
        tags.addAll(context.getParentTags());

        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null) {
            tags.addAll(Collections2.filter(Arrays.asList(apiOperation.tags()),
                new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        return StringUtils.isNotBlank(input);
                    }
                }));
        }

        for (String tag : tags) {
            operation.tag(tag);
        }

    }

    private static final String SUCCESSFUL_OPERATION = "";

    private static boolean isValidResponse(Type type) {
        final JavaType javaType = TypeFactory.defaultInstance().constructType(type);
        return !ReflectionUtils.isVoid(javaType);
    }

    private static Map<String, Property> parseResponseHeaders(ReaderContext context,
                                                              ResponseHeader[] headers) {
        Map<String, Property> responseHeaders = null;
        for (ResponseHeader header : headers) {
            final String name = header.name();
            if (StringUtils.isNotEmpty(name)) {
                if (responseHeaders == null) {
                    responseHeaders = new HashMap<String, Property>();
                }
                final Class<?> cls = header.response();
                if (!ReflectionUtils.isVoid(cls)) {
                    final Property property = ModelConverters.getInstance().readAsProperty(cls);
                    if (property != null) {
                        final Property responseProperty = ContainerWrapper.wrapContainer(
                            header.responseContainer(), property, ContainerWrapper.ARRAY,
                            ContainerWrapper.LIST, ContainerWrapper.SET);
                        responseProperty.setDescription(header.description());
                        responseHeaders.put(name, responseProperty);
                        appendModels(context.getSwagger(), cls);
                    }
                }
            }
        }
        return responseHeaders;
    }

    private static void appendModels(Swagger swagger, Type type) {
        final Map<String, Model> models = ModelConverters.getInstance().readAll(type);
        for (Map.Entry<String, Model> entry : models.entrySet()) {
            swagger.model(entry.getKey(), entry.getValue());
        }
    }

    private static Type getResponseType(Method method) {
        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null && !ReflectionUtils.isVoid(apiOperation.response())) {
            return apiOperation.response();
        } else {
            return method.getGenericReturnType();
        }
    }

    private static String getResponseContainer(ApiOperation apiOperation) {
        return apiOperation == null ? null
            : StringUtils.defaultIfBlank(apiOperation.responseContainer(), null);
    }

    @Override
    public void applyResponses(ReaderContext context, Operation operation, Method method) {
        final Map<Integer, Response> result = new HashMap<Integer, Response>();

        final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null && StringUtils.isNotBlank(apiOperation.responseReference())) {
            final Response response = new Response().description(SUCCESSFUL_OPERATION);
            response.schema(new RefProperty(apiOperation.responseReference()));
            result.put(apiOperation.code(), response);
        }

        final Type responseType = getResponseType(method);
        if (isValidResponse(responseType)) {
            final Property property = ModelConverters.getInstance().readAsProperty(responseType);
            if (property != null) {
                final Property responseProperty = ContainerWrapper
                    .wrapContainer(getResponseContainer(apiOperation), property);
                final int responseCode = apiOperation == null ? 200 : apiOperation.code();
                final Map<String, Property> defaultResponseHeaders = apiOperation == null
                    ? Collections.<String, Property> emptyMap()
                    : parseResponseHeaders(context, apiOperation.responseHeaders());
                final Response response = new Response().description(SUCCESSFUL_OPERATION)
                    .schema(responseProperty).headers(defaultResponseHeaders);
                result.put(responseCode, response);
                appendModels(context.getSwagger(), responseType);
            }
        }

        final ApiResponses responseAnnotation = ReflectionUtils.getAnnotation(method,
            ApiResponses.class);
        if (responseAnnotation != null) {
            for (ApiResponse apiResponse : responseAnnotation.value()) {
                final Map<String, Property> responseHeaders = parseResponseHeaders(context,
                    apiResponse.responseHeaders());

                final Response response = new Response().description(apiResponse.message())
                    .headers(responseHeaders);

                if (StringUtils.isNotEmpty(apiResponse.reference())) {
                    response.schema(new RefProperty(apiResponse.reference()));
                } else if (!ReflectionUtils.isVoid(apiResponse.response())) {
                    final Type type = apiResponse.response();
                    final Property property = ModelConverters.getInstance().readAsProperty(type);
                    if (property != null) {
                        response.schema(ContainerWrapper
                            .wrapContainer(apiResponse.responseContainer(), property));
                        appendModels(context.getSwagger(), type);
                    }
                }
                result.put(apiResponse.code(), response);
            }
        }

        for (Map.Entry<Integer, Response> responseEntry : result.entrySet()) {
            if (responseEntry.getKey() == 0) {
                operation.defaultResponse(responseEntry.getValue());
            } else {
                operation.response(responseEntry.getKey(), responseEntry.getValue());
            }
        }

    }

    @Override
    public void applyParameters(ReaderContext context, Operation operation, Type type,
                                Annotation[] annotations) {
    }

    private void applyParametersV2(ReaderContext context, Operation operation, String name,
                                   Type type, Class<?> cls, Annotation[] annotations,
                                   Annotation[] interfaceParamAnnotations) {
        Annotation apiParam = null;
        if (annotations != null) {
            for (Annotation annotation : interfaceParamAnnotations) {
                if (annotation instanceof ApiParam) {
                    apiParam = annotation;
                    break;
                }
            }
            if (null == apiParam) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof ApiParam) {
                        apiParam = annotation;
                        break;
                    }
                }
            }
        }
        final Parameter parameter = readParam(context.getSwagger(), type, cls,
            null == apiParam ? null : (ApiParam) apiParam);
        if (parameter != null) {
            parameter.setName(null == name ? parameter.getName() : name);
            operation.parameter(parameter);
        }
    }

    private Parameter readParam(Swagger swagger, Type type, Class<?> cls, ApiParam param) {
        PrimitiveType fromType = PrimitiveType.fromType(type);
        final Parameter para = null == fromType ? new BodyParameter() : new QueryParameter();
        Parameter parameter = ParameterProcessor.applyAnnotations(swagger, para, type,
            null == param ? new ArrayList<Annotation>()
                : Collections.<Annotation> singletonList(param));
        if (parameter instanceof AbstractSerializableParameter) {
            final AbstractSerializableParameter<?> p = (AbstractSerializableParameter<?>) parameter;
            if (p.getType() == null) {
                p.setType(null == fromType ? "string" : fromType.getCommonName());
            }
            p.setRequired(p.getRequired() || cls.isPrimitive());
        } else {
            //hack: Get the from data model paramter from BodyParameter
            BodyParameter bp = (BodyParameter) parameter;
            bp.setIn("body");
            Property property = ModelConverters.getInstance().readAsProperty(type);
            final Map<PropertyBuilder.PropertyId, Object> args = new EnumMap<PropertyBuilder.PropertyId, Object>(
                PropertyBuilder.PropertyId.class);
            bp.setSchema(PropertyBuilder.toModel(PropertyBuilder.merge(property, args)));
        }
        return parameter;
    }

    @Override
    public void applyParameters(ReaderContext context, Operation operation, Method method,
                                Method interfaceMethod) {
        try {
            String[] parameterNames = getMethodParameterNames(method);
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Annotation[][] interfaceParamAnnotations = interfaceMethod.getParameterAnnotations();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                applyParametersV2(context, operation,
                    null == parameterNames ? null : parameterNames[i], genericParameterTypes[i], parameterTypes[i],
                    parameterAnnotations[i], interfaceParamAnnotations[i]);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    private String[] getMethodParameterNames(Method method) {
        String[] parameterNames = localVariableTableParameterNameDiscoverer.getParameterNames(method);
        if (CommonUtils.isEmpty(parameterNames)) {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            parameterNames = new String[parameters.length];
            int ix = 0;
            for (java.lang.reflect.Parameter parameter : parameters) {
                parameterNames[ix] = parameter.getName();
                ix++;
            }
        }
        return parameterNames;
    }

    @Override
    public void applyImplicitParameters(ReaderContext context, Operation operation, Method method) {
        final ApiImplicitParams implicitParams = method.getAnnotation(ApiImplicitParams.class);
        if (implicitParams != null && implicitParams.value().length > 0) {
            for (ApiImplicitParam param : implicitParams.value()) {
                final Parameter p = readImplicitParam(context.getSwagger(), param);
                if (p != null) {
                    operation.parameter(p);
                }
            }
        }
    }

    private Parameter readImplicitParam(Swagger swagger, ApiImplicitParam param) {
        PrimitiveType fromType = PrimitiveType.fromName(param.paramType());
        final Parameter p = null == fromType ? new FormParameter() : new QueryParameter();
        final Type type = ReflectionUtils.typeFromString(param.dataType());
        return ParameterProcessor.applyAnnotations(swagger, p, type == null ? String.class : type,
            Collections.<Annotation> singletonList(param));
    }

    @Override
    public void applyExtensions(ReaderContext context, Operation operation, Method method) {
        final ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
        if (apiOperation != null) {
            operation.getVendorExtensions()
                .putAll(BaseReaderUtils.parseExtensions(apiOperation.extensions()));
        }

    }

    enum ContainerWrapper {
        LIST("list") {
            @Override
            protected Property doWrap(Property property) {
                return new ArrayProperty(property);
            }
        },
        ARRAY("array") {
            @Override
            protected Property doWrap(Property property) {
                return new ArrayProperty(property);
            }
        },
        MAP("map") {
            @Override
            protected Property doWrap(Property property) {
                return new MapProperty(property);
            }
        },
        SET("set") {
            @Override
            protected Property doWrap(Property property) {
                ArrayProperty arrayProperty = new ArrayProperty(property);
                arrayProperty.setUniqueItems(true);
                return arrayProperty;
            }
        };

        private final String container;

        ContainerWrapper(String container) {
            this.container = container;
        }

        public static Property wrapContainer(String container, Property property,
                                             ContainerWrapper... allowed) {
            final Set<ContainerWrapper> tmp = allowed.length > 0
                ? EnumSet.copyOf(Arrays.asList(allowed))
                : EnumSet.allOf(ContainerWrapper.class);
            for (ContainerWrapper wrapper : tmp) {
                final Property prop = wrapper.wrap(container, property);
                if (prop != null) {
                    return prop;
                }
            }
            return property;
        }

        public Property wrap(String container, Property property) {
            if (this.container.equalsIgnoreCase(container)) {
                return doWrap(property);
            }
            return null;
        }

        protected abstract Property doWrap(Property property);
    }

}
