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
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: GrpcTestService.proto

package com.alipay.sofa.rpc.model.grpc;

public final class GrpcTestServiceProto {
    private GrpcTestServiceProto() {
    }

    public static void registerAllExtensions(
                                             com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(
                                             com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor                internal_static_GrpcTestService_Request_String_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_GrpcTestService_Request_String_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor                internal_static_GrpcTestService_Response_String_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_GrpcTestService_Response_String_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
            getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
    static {
        String[] descriptorData = {
            "\n\025GrpcTestService.proto\".\n\036GrpcTestServi" +
                "ce_Request_String\022\014\n\004name\030\001 \001(\t\"1\n\037GrpcT" +
                "estService_Response_String\022\016\n\006result\030\001 \001" +
                "(\t2\203\003\n\017GrpcTestService\022P\n\treqString\022\037.Gr" +
                "pcTestService_Request_String\032 .GrpcTestS" +
                "ervice_Response_String\"\000\022^\n\025reqStringCli" +
                "entStream\022\037.GrpcTestService_Request_Stri" +
                "ng\032 .GrpcTestService_Response_String\"\000(\001" +
                "\022^\n\025reqStringServerStream\022\037.GrpcTestServ" +
                "ice_Request_String\032 .GrpcTestService_Res" +
                "ponse_String\"\0000\001\022^\n\023reqStringBothStream\022" +
                "\037.GrpcTestService_Request_String\032 .GrpcT" +
                "estService_Response_String\"\000(\0010\001B;\n\036com." +
                "alipay.sofa.rpc.model.grpcB\024GrpcTestServ" +
                "iceProtoP\001\210\001\001b\006proto3"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                                                                                   com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor
            .internalBuildGeneratedFileFrom(descriptorData,
                new com.google.protobuf.Descriptors.FileDescriptor[] {
                }, assigner);
        internal_static_GrpcTestService_Request_String_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_GrpcTestService_Request_String_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GrpcTestService_Request_String_descriptor,
                    new String[] { "Name", });
        internal_static_GrpcTestService_Response_String_descriptor =
                getDescriptor().getMessageTypes().get(1);
        internal_static_GrpcTestService_Response_String_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GrpcTestService_Response_String_descriptor,
                    new String[] { "Result", });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
