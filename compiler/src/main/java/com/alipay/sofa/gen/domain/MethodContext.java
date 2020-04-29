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

/**
 * Template class for proto RPC objects.
 */
public class MethodContext {
    private String  methodName;
    private String  inputType;
    private String  outputType;
    private boolean deprecated;
    private boolean isManyInput;
    private boolean isManyOutput;
    private String  reactiveCallsMethodName;
    private String  grpcCallsMethodName;
    private int     methodNumber;
    private String  javaDoc;

    // This method mimics the upper-casing method ogf gRPC to ensure compatibility
    // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
    public String methodNameUpperUnderscore() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            s.append(Character.toUpperCase(c));
            if ((i < methodName.length() - 1) && Character.isLowerCase(c)
                && Character.isUpperCase(methodName.charAt(i + 1))) {
                s.append('_');
            }
        }
        return s.toString();
    }

    public String methodNamePascalCase() {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
    }

    public String methodNameCamelCase() {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isManyInput() {
        return isManyInput;
    }

    public void setManyInput(boolean manyInput) {
        isManyInput = manyInput;
    }

    public boolean isManyOutput() {
        return isManyOutput;
    }

    public void setManyOutput(boolean manyOutput) {
        isManyOutput = manyOutput;
    }

    public String getReactiveCallsMethodName() {
        return reactiveCallsMethodName;
    }

    public void setReactiveCallsMethodName(String reactiveCallsMethodName) {
        this.reactiveCallsMethodName = reactiveCallsMethodName;
    }

    public String getGrpcCallsMethodName() {
        return grpcCallsMethodName;
    }

    public void setGrpcCallsMethodName(String grpcCallsMethodName) {
        this.grpcCallsMethodName = grpcCallsMethodName;
    }

    public int getMethodNumber() {
        return methodNumber;
    }

    public void setMethodNumber(int methodNumber) {
        this.methodNumber = methodNumber;
    }

    public String getJavaDoc() {
        return javaDoc;
    }

    public void setJavaDoc(String javaDoc) {
        this.javaDoc = javaDoc;
    }

    @Override
    public String toString() {
        return "MethodContext{" + "methodName='" + methodName + '\'' + ", inputType='" + inputType
            + '\'' + ", outputType='" + outputType + '\'' + ", deprecated=" + deprecated
            + ", isManyInput=" + isManyInput + ", isManyOutput=" + isManyOutput
            + ", reactiveCallsMethodName='" + reactiveCallsMethodName + '\''
            + ", grpcCallsMethodName='" + grpcCallsMethodName + '\'' + ", methodNumber="
            + methodNumber + ", javaDoc='" + javaDoc + '\'' + '}';
    }
}