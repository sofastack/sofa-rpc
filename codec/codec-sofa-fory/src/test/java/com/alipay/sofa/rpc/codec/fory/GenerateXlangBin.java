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
package com.alipay.sofa.rpc.codec.fory;

import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoRequest;
import org.apache.fory.Fory;
import org.apache.fory.config.Language;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Generates the xlang-mode Fory binary file for cross-language interop tests.
 * This creates a DemoRequest(name="hello from java") serialized with xlang mode.
 */
public class GenerateXlangBin {
    public static void main(String[] args) throws IOException {
        // Build xlang-mode Fory instance
        final String typeTag = "com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoRequest";
        Fory xlangFory = Fory.builder()
            .withLanguage(Language.XLANG)
            .withRefTracking(true)
            .requireClassRegistration(true)
            .build();
        xlangFory.register(DemoRequest.class, typeTag);

        // Create and serialize DemoRequest
        DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("hello from java");
        byte[] bytes = xlangFory.serialize(demoRequest);

        // Write to output file
        String outputPath = System.getProperty("user.dir") + "/codec/codec-sofa-fory/src/test/resources/xlang/fory_xlang_from_java.bin";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(bytes);
        }

        System.out.println("Generated " + outputPath + " (" + bytes.length + " bytes)");
    }
}
