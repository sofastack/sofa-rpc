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
package com.alipay.sofa.gen.grpc;

import com.alipay.sofa.gen.base.AbstractGenerator;
import com.salesforce.jprotoc.ProtocPlugin;

public class SofaGrpcGenerator extends AbstractGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new SofaGrpcGenerator());
        } else {
            ProtocPlugin.debug(new SofaGrpcGenerator(), args[0]);
        }
    }

    @Override
    protected String getClassPrefix() {
        return "Sofa";
    }

    protected String getClassSuffix() {
        return "Grpc";
    }
}
