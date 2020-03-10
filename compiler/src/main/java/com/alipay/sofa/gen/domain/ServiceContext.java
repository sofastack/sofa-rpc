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
package com.alipay.sofa.gen.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template class for proto Service objects.
 */
public class ServiceContext {
    private String              fileName;
    private String              protoName;
    private String              packageName;
    private String              className;
    private String              serviceName;
    private boolean             deprecated;
    private String              javaDoc;
    private List<MethodContext> methods     = new ArrayList<>();
    private  Set<String>         methodTypes = new HashSet<>();

    public List<MethodContext> unaryRequestMethods() {
        List<MethodContext> list = new ArrayList<>();
        for (MethodContext m : methods) {
            if (!m.isManyInput()) {
                list.add(m);
            }
        }
        return list;
    }

    public List<MethodContext> unaryMethods() {
        List<MethodContext> list = new ArrayList<>();
        for (MethodContext m : methods) {
            if ((!m.isManyInput() && !m.isManyOutput())) {
                list.add(m);
            }
        }
        return list;
    }

    public List<MethodContext> serverStreamingMethods() {
        List<MethodContext> list = new ArrayList<>();
        for (MethodContext m : methods) {
            if (!m.isManyInput() && m.isManyOutput()) {
                list.add(m);
            }
        }
        return list;
    }

    public List<MethodContext> biStreamingMethods() {
        List<MethodContext> list = new ArrayList<>();
        for (MethodContext m : methods) {
            if (m.isManyInput()) {
                list.add(m);
            }
        }
        return list;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProtoName() {
        return protoName;
    }

    public void setProtoName(String protoName) {
        this.protoName = protoName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getJavaDoc() {
        return javaDoc;
    }

    public void setJavaDoc(String javaDoc) {
        this.javaDoc = javaDoc;
    }

    public List<MethodContext> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodContext> methods) {
        this.methods = methods;
    }

    public Set<String> getMethodTypes() {
        return methodTypes;
    }

    public void setMethodTypes(Set<String> methodTypes) {
        this.methodTypes = methodTypes;
    }
}