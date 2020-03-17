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
package com.alipay.sofa.gen.base;

import com.alipay.sofa.gen.domain.MethodContext;
import com.alipay.sofa.gen.domain.ServiceContext;
import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractGenerator extends Generator {

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS  = 4;

    protected abstract String getClassPrefix();

    protected abstract String getClassSuffix();

    private String getServiceJavaDocPrefix() {
        return "    ";
    }

    private String getMethodJavaDocPrefix() {
        return "        ";
    }

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request)
            throws GeneratorException {
        final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<FileDescriptorProto> protosToGenerate = new ArrayList<>();
        for (FileDescriptorProto protoFile : request.getProtoFileList()) {
            if (request.getFileToGenerateList().contains(protoFile.getName())) {
                protosToGenerate.add(protoFile);
            }
        }

        List<ServiceContext> services = findServices(protosToGenerate, typeMap);
        return generateFiles(services);
    }

    private List<ServiceContext> findServices(List<FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();
        for (FileDescriptorProto fileProto : protos) {
            for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
                ServiceContext serviceContext = buildServiceContext(
                        fileProto.getService(serviceNumber),
                        typeMap,
                        fileProto.getSourceCodeInfo().getLocationList(),
                        serviceNumber
                );
                serviceContext.setProtoName(fileProto.getName());
                serviceContext.setPackageName(extractPackageName(fileProto));
                contexts.add(serviceContext);
            }
        }

        return contexts;
    }

    /**
     * fetch the package name
     *
     * @param proto
     * @return
     */
    private String extractPackageName(FileDescriptorProto proto) {
        FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    /**
     * construct the context
     *
     * @param serviceProto
     * @param typeMap
     * @param locations
     * @param serviceNumber
     * @return
     */
    private ServiceContext buildServiceContext(ServiceDescriptorProto serviceProto, ProtoTypeMap typeMap, List<Location> locations,
                                               int serviceNumber) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setFileName(getClassPrefix() + serviceProto.getName() + getClassSuffix() + ".java");
        serviceContext.setClassName(getClassPrefix() + serviceProto.getName() + getClassSuffix());
        serviceContext.setServiceName(serviceProto.getName());
        serviceContext.setDeprecated(serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated());

        List<Location> allLocationsForService = new ArrayList<>();
        for (Location location : locations) {
            if (location.getPathCount() >= 2 &&
                    location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                    location.getPath(1) == serviceNumber) {
                allLocationsForService.add(location);
            }
        }

        Optional<Location> found = Optional.empty();
        for (Location location : allLocationsForService) {
            if (location.getPathCount() == SERVICE_NUMBER_OF_PATHS) {
                found = Optional.of(location);
                break;
            }
        }
        Location locationDefault = Location.getDefaultInstance();
        Location serviceLocation = found
                .orElseGet(new Supplier<Location>() {
                    @Override
                    public Location get() {
                        return locationDefault;
                    }
                });
        serviceContext.setJavaDoc(getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix()));

        for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
            MethodContext methodContext = buildMethodContext(serviceProto.getMethod(methodNumber), typeMap, locations, methodNumber
            );
            serviceContext.getMethods().add(methodContext);
            serviceContext.getMethodTypes().add(methodContext.getInputType());
            serviceContext.getMethodTypes().add(methodContext.getOutputType());
        }
        return serviceContext;
    }

    private MethodContext buildMethodContext(MethodDescriptorProto methodProto, ProtoTypeMap typeMap, List<Location> locations,
                                             int methodNumber) {
        MethodContext methodContext = new MethodContext();
        methodContext.setMethodName(lowerCaseFirst(methodProto.getName()));
        methodContext.setInputType(typeMap.toJavaTypeName(methodProto.getInputType()));
        methodContext.setOutputType(typeMap.toJavaTypeName(methodProto.getOutputType()));
        methodContext.setDeprecated(methodProto.getOptions() != null && methodProto.getOptions().getDeprecated());
        methodContext.setManyInput(methodProto.getClientStreaming());
        methodContext.setManyOutput(methodProto.getServerStreaming());
        methodContext.setMethodNumber(methodNumber);

        Optional<Location> found = Optional.empty();
        for (Location location : locations) {
            if (location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                    location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber) {
                found = Optional.of(location);
                break;
            }
        }
        Location methodLocation = found
                .orElseGet(Location::getDefaultInstance);
        methodContext.setJavaDoc(getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix()));

        if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.setReactiveCallsMethodName("oneToOne");
            methodContext.setGrpcCallsMethodName("asyncUnaryCall");
        }
        if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.setReactiveCallsMethodName("oneToMany");
            methodContext.setGrpcCallsMethodName("asyncServerStreamingCall");
        }
        if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.setReactiveCallsMethodName("manyToOne");
            methodContext.setGrpcCallsMethodName("asyncClientStreamingCall");
        }
        if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.setReactiveCallsMethodName("manyToMany");
            methodContext.setGrpcCallsMethodName("asyncBidiStreamingCall");
        }
        return methodContext;
    }

    private String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * generate file
     *
     * @param services
     * @return
     */
    private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<ServiceContext> services) {
        List<PluginProtos.CodeGeneratorResponse.File> list = new ArrayList<>();
        for (ServiceContext service : services) {
            PluginProtos.CodeGeneratorResponse.File file = buildFile(service);
            list.add(file);
        }
        return list;
    }

    /**
     * 构造文件了
     *
     * @param context
     * @return
     */
    private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context) {
        final String mustAcheFile = getClassPrefix() + getClassSuffix() + "Stub.mustache";
        String content = applyTemplate(mustAcheFile, context);
        return PluginProtos.CodeGeneratorResponse.File.newBuilder()
            .setName(constructAbsoluteFileName(context)).setContent(content).build();
    }

    /**
     * 构造绝对路径
     *
     * @param ctx
     * @return
     */
    private String constructAbsoluteFileName(ServiceContext ctx) {
        String dir = ctx.getPackageName().replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.getFileName();
        } else {
            return dir + "/" + ctx.getFileName();
        }
    }

    /**
     * 注释
     *
     * @param location
     * @return
     */
    private String getComments(Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location
            .getLeadingComments();
    }

    /**
     * Java doc
     *
     * @param comments
     * @param prefix
     * @return
     */
    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n").append(prefix).append(" * <pre>\n");
            for (String line : HtmlEscapers.htmlEscaper().escape(comments).split("\n")) {
                String replace = line.replace("*/", "&#42;&#47;").replace("*", "&#42;");
                builder.append(prefix).append(" * ").append(replace).append("\n");
            }
            builder.append(prefix).append(" * </pre>\n").append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

}
