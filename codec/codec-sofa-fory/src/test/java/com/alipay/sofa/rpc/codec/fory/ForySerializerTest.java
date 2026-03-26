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

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.fory.model.blacklist.BlackListClass;
import com.alipay.sofa.rpc.codec.fory.model.none.NoneClassHasBlackClass;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoRequest;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoResponse;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoService;
import com.alipay.sofa.rpc.codec.fory.model.whitelist.WhiteClassHasBlackClass;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import org.apache.fory.Fory;
import org.apache.fory.config.Language;
import org.apache.fory.memory.MemoryBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link ForySerializer}.
 *
 * @author <a href="mailto:sunhailin.shl@antgroup.com">sunhailin-Leo</a>
 */
public class ForySerializerTest {

    private final ForySerializer serializer = (ForySerializer) ExtensionLoaderFactory.getExtensionLoader(
                                                Serializer.class).getExtension("fory");

    @Test
    public void encodeAndDecode() {
        try {
            serializer.encode(null, null);
            Assert.fail();
        } catch (Exception e) {

        }

        DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("a");
        AbstractByteBuf byteBuf = serializer.encode(demoRequest, null);
        DemoRequest req2 = (DemoRequest) serializer.decode(byteBuf, DemoRequest.class, null);
        Assert.assertEquals(demoRequest.getName(), req2.getName());

        AbstractByteBuf data = serializer.encode("xxx", null);
        String dst = (String) serializer.decode(data, String.class, null);
        Assert.assertEquals("xxx", dst);

        try {
            serializer.decode(data, null, null);
            Assert.fail();
        } catch (Exception e) {
        }

        try {
            serializer.decode(data, "", null);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void testSofaRequest() throws Exception {
        SofaRequest request = buildRequest();
        AbstractByteBuf data = serializer.encode(request, null);

        serializer.encode("123456", null);
        SofaRequest decode = (SofaRequest) serializer.decode(data, SofaRequest.class, null);
        assertEqualsSofaRequest(request, decode);

        SofaRequest newRequest = new SofaRequest();
        serializer.decode(data, newRequest, null);
        assertEqualsSofaRequest(request, newRequest);

        // null request
        newRequest = new SofaRequest();
        try {
            serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newRequest, null);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void testSofaResponse() throws Exception {
        SofaResponse response = new SofaResponse();
        final DemoResponse demoAppResponse = new DemoResponse();
        demoAppResponse.setWord("result");
        response.setAppResponse(demoAppResponse);
        AbstractByteBuf data = serializer.encode(response, null);

        try {
            serializer.decode(data, null, null);
            Assert.fail();
        } catch (Exception e) {

        }

        try {
            serializer.decode(data, new Object(), null);
            Assert.fail();
        } catch (Exception e) {

        }

        SofaResponse decode = (SofaResponse) serializer.decode(data, SofaResponse.class, null);
        Assert.assertFalse(decode.isError());
        Assert.assertEquals(response.getAppResponse(), decode.getAppResponse());
        Assert.assertEquals("result", ((DemoResponse) decode.getAppResponse()).getWord());

        // success response via template
        SofaResponse newResponse = new SofaResponse();
        serializer.decode(data, newResponse, null);
        Assert.assertFalse(newResponse.isError());
        Assert.assertEquals(response.getAppResponse(), newResponse.getAppResponse());
        Assert.assertEquals("result", ((DemoResponse) newResponse.getAppResponse()).getWord());

        // null response
        newResponse = new SofaResponse();
        try {
            serializer.decode(new ByteArrayWrapperByteBuf(new byte[0]), newResponse, null);
            Assert.fail();
        } catch (Exception e) {

        }

        // error response
        response = new SofaResponse();
        response.setErrorMsg("1233");
        data = serializer.encode(response, null);
        newResponse = new SofaResponse();
        serializer.decode(data, newResponse, null);
        Assert.assertTrue(newResponse.isError());
        Assert.assertEquals(response.getErrorMsg(), newResponse.getErrorMsg());
    }

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(DemoService.class.getName());
        request.setMethodName("say");
        request.setMethod(DemoService.class.getMethod("say", DemoRequest.class));
        final DemoRequest demoRequest = new DemoRequest();
        demoRequest.setName("name");
        request.setMethodArgs(new Object[] { demoRequest });
        request.setMethodArgSigs(new String[] { DemoRequest.class.getCanonicalName() });
        request.setTargetServiceUniqueName(DemoService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 23);
        request.setTimeout(1024);
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "xxx");
        map.put("b", "yyy");
        request.addRequestProp(RemotingConstants.RPC_TRACE_NAME, map);
        request.setSofaResponseCallback(new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {

            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {

            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {

            }
        });
        return request;
    }

    private void assertEqualsSofaRequest(SofaRequest request, SofaRequest decode) {
        Assert.assertEquals(request.getInterfaceName(), decode.getInterfaceName());
        Assert.assertEquals(request.getMethodName(), decode.getMethodName());
        Assert.assertArrayEquals(request.getMethodArgSigs(), decode.getMethodArgSigs());
        Assert.assertEquals(request.getMethodArgs().length, decode.getMethodArgs().length);
        Assert.assertEquals("name", ((DemoRequest) decode.getMethodArgs()[0]).getName());
        Assert.assertEquals(request.getTargetServiceUniqueName(), decode.getTargetServiceUniqueName());
        Assert.assertEquals(request.getTargetAppName(), decode.getTargetAppName());
        Assert.assertEquals(request.getRequestProp(RemotingConstants.RPC_TRACE_NAME),
            decode.getRequestProp(RemotingConstants.RPC_TRACE_NAME));
    }

    @Test
    public void testChecker() throws Exception {
        // default fory checkMode is STRICT
        WhiteClassHasBlackClass whiteClassNullBlackClass = new WhiteClassHasBlackClass();
        NoneClassHasBlackClass noneClassNullBlackClass = new NoneClassHasBlackClass();

        BlackListClass blackListClass = new BlackListClass();
        WhiteClassHasBlackClass whiteClassHasBlackClass = new WhiteClassHasBlackClass();
        whiteClassHasBlackClass.setBlackListClass(blackListClass);
        NoneClassHasBlackClass noneClassHasBlackClass = new NoneClassHasBlackClass();
        noneClassHasBlackClass.setBlackListClass(blackListClass);

        try {
            serializer.encode(noneClassNullBlackClass, null);
            Assert.fail();
        } catch (Exception e) {

        }

        try {
            serializer.encode(noneClassHasBlackClass, null);
            Assert.fail();
        } catch (Exception e) {

        }

        try {
            serializer.encode(blackListClass, null);
            Assert.fail();
        } catch (Exception e) {

        }

        serializer.encode(whiteClassNullBlackClass, null);
        try {
            serializer.encode(whiteClassHasBlackClass, null);
            Assert.fail();
        } catch (Exception e) {

        }

        // test WARN mode
        System.getProperties().put("sofa.rpc.codec.serialize.checkMode", "WARN");
        try {
            ForySerializer warnForySerializer = new ForySerializer();

            warnForySerializer.encode(noneClassNullBlackClass, null);

            try {
                warnForySerializer.encode(noneClassHasBlackClass, null);
                Assert.fail();
            } catch (Exception e) {

            }

            try {
                warnForySerializer.encode(blackListClass, null);
                Assert.fail();
            } catch (Exception e) {

            }

            warnForySerializer.encode(whiteClassNullBlackClass, null);
            try {
                warnForySerializer.encode(whiteClassHasBlackClass, null);
                Assert.fail();
            } catch (Exception e) {

            }
        } finally {
            System.getProperties().remove("sofa.rpc.codec.serialize.checkMode");
        }

        // test DISABLE mode
        System.getProperties().put("sofa.rpc.codec.serialize.checkMode", "DISABLE");
        try {
            ForySerializer disableForySerializer = new ForySerializer();
            disableForySerializer.encode(noneClassNullBlackClass, null);
            disableForySerializer.encode(noneClassHasBlackClass, null);
            disableForySerializer.encode(blackListClass, null);
            disableForySerializer.encode(whiteClassNullBlackClass, null);
            disableForySerializer.encode(whiteClassHasBlackClass, null);
        } finally {
            System.getProperties().remove("sofa.rpc.codec.serialize.checkMode");
        }
    }

    /**
     * Builds a dedicated xlang-mode Fory instance for cross-language interop tests.
     * This instance is separate from the JAVA-mode {@link ForySerializer}.
     */
    private Fory buildXlangFory() {
        final String typeTag = "com.alipay.sofa.rpc.codec.fory.model.whitelist.DemoRequest";
        Fory xlangFory = Fory.builder()
            .withLanguage(Language.XLANG)
            .withRefTracking(true)
            .requireClassRegistration(true)
            .build();
        xlangFory.register(DemoRequest.class, typeTag);
        return xlangFory;
    }

    /**
     * Cross-language test: Java reads bytes produced by Python.
     *
     * <p>Prerequisite: Run {@code TestForyXlangInterop#test_python_to_java_serialize} in Python
     * first. It serializes a {@code DemoRequest(name="hello from python")} with xlang mode and
     * writes the bytes to {@code src/test/resources/xlang/fory_xlang_from_python.bin} in this
     * Java project. Commit that file so it is available on the classpath at test time.
     */
    @Test
    public void testXlangReadFromPython() throws IOException {
        final String classpathResource = "/xlang/fory_xlang_from_python.bin";
        Fory xlangFory = buildXlangFory();

        try (InputStream pythonBytesStream = ForySerializerTest.class.getResourceAsStream(classpathResource)) {
            Assert.assertNotNull(
                "Classpath resource not found: " + classpathResource
                    + ". Run TestForyXlangInterop#test_python_to_java_serialize in Python first,"
                    + " then commit the file to src/test/resources/xlang/.",
                pythonBytesStream);

            // Java 8 compatible: read all bytes from InputStream manually.
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = pythonBytesStream.read(chunk)) != -1) {
                byteBuffer.write(chunk, 0, bytesRead);
            }
            byte[] pythonBytes = byteBuffer.toByteArray();

            Assert.assertTrue("Expected non-empty bytes from Python", pythonBytes.length > 0);
            MemoryBuffer readBuffer = MemoryBuffer.fromByteArray(pythonBytes);
            DemoRequest fromPython = (DemoRequest) xlangFory.deserialize(readBuffer);
            Assert.assertEquals("hello from python", fromPython.getName());
        }
    }

    /**
     * Cross-language test: Java reads the pre-generated bytes produced by the Python test
     * ({@code TestForyXlangInterop#test_python_to_java_serialize}) and deserializes them.
     *
     * <p>The bin file {@code src/test/resources/xlang/fory_xlang_from_java.bin} is committed to
     * this project and loaded from the classpath, so no external dependency is needed.
     * Expected deserialized value: {@code DemoRequest(name="hello from java")}.
     */
    @Test
    public void testXlangReadFromJava() throws IOException {
        final String classpathResource = "/xlang/fory_xlang_from_java.bin";
        Fory xlangFory = buildXlangFory();

        try (InputStream javaBytesStream = ForySerializerTest.class.getResourceAsStream(classpathResource)) {
            Assert.assertNotNull("Classpath resource not found: " + classpathResource, javaBytesStream);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int bytesRead;
            while ((bytesRead = javaBytesStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            byte[] javaBytes = buffer.toByteArray();

            Assert.assertTrue("Expected non-empty bytes from Java", javaBytes.length > 0);
            MemoryBuffer memoryBuffer = MemoryBuffer.fromByteArray(javaBytes);
            DemoRequest fromJava = (DemoRequest) xlangFory.deserialize(memoryBuffer);
            Assert.assertEquals("hello from java", fromJava.getName());
        }
    }
}
