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
package com.alipay.sofa.rpc.codec.sofahessian.serialize;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.codec.sofahessian.GenericMultipleClassLoaderSofaSerializerFactory;
import com.alipay.sofa.rpc.codec.sofahessian.mock.MockError;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.caucho.hessian.io.Hessian2Output;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.sofa.rpc.codec.sofahessian.serialize.GenericCustomThrowableDeterminerTest.setGenericThrowException;

/**
 *
 * @author xingqi
 * @version : SofaResponseHessianSerializerTest.java, v 0.1 2022年10月20日 4:07 PM xingqi Exp $
 */
public class SofaResponseHessianSerializerTest {

    @Test
    public void testCustomThrowableDeserializer() throws Exception {
        GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();
        SofaResponseHessianSerializer serializer = new SofaResponseHessianSerializer(null, factory);

        ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(bsOut);
        hessian2Output.setSerializerFactory(factory);

        SofaResponse sofaResponse = new SofaResponse();
        MockError mockError = new MockError("MockError");
        sofaResponse.setAppResponse(mockError);
        hessian2Output.writeObject(sofaResponse);
        hessian2Output.flush();

        ByteArrayWrapperByteBuf bsIn = new ByteArrayWrapperByteBuf(bsOut.toByteArray());
        Map<String, String> ctx = new HashMap<>();
        ctx.put(RemotingConstants.HEAD_GENERIC_TYPE, "2");
        SofaResponse sofaResponse2 = new SofaResponse();
        serializer.decodeObjectByTemplate(bsIn, ctx, sofaResponse2);
        Assert.assertTrue(sofaResponse2.getAppResponse() instanceof GenericObject);
        Assert.assertEquals("MockError", ((GenericObject) sofaResponse2.getAppResponse()).getField("detailMessage"));
    }

    @Test
    public void testCustomThrowableDeserializerEnabled() throws Exception {
        setGenericThrowException(true);
        try {
            GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();
            SofaResponseHessianSerializer serializer = new SofaResponseHessianSerializer(null, factory);

            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            Hessian2Output hessian2Output = new Hessian2Output(bsOut);
            hessian2Output.setSerializerFactory(factory);

            SofaResponse sofaResponse = new SofaResponse();
            MockError mockError = new MockError("MockError");
            sofaResponse.setAppResponse(mockError);
            hessian2Output.writeObject(sofaResponse);
            hessian2Output.flush();

            ByteArrayWrapperByteBuf bsIn = new ByteArrayWrapperByteBuf(bsOut.toByteArray());
            Map<String, String> ctx = new HashMap<>();
            ctx.put(RemotingConstants.HEAD_GENERIC_TYPE, "2");
            SofaResponse sofaResponse2 = new SofaResponse();
            serializer.decodeObjectByTemplate(bsIn, ctx, sofaResponse2);
            Assert.assertTrue(sofaResponse2.getAppResponse() instanceof MockError);
            Assert.assertEquals("MockError", ((MockError) sofaResponse2.getAppResponse()).getMessage());
        } finally {
            setGenericThrowException(false);
        }
    }

    @Test
    public void testCustomThrowableDeserializerEnabledForIncompatible() throws Exception {
        setGenericThrowException(true);
        try {
            GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();
            SofaResponseHessianSerializer serializer = new SofaResponseHessianSerializer(null, factory);
            // 将com.alipay.sofa.rpc.codec.sofahessian.mock.DeprecatedMockError重命名为com.alipay.sofa.rpc.codec.sofahessian.mock.MockError
            // SofaResponse sofaResponse = new SofaResponse();
            // DeprecatedMockError mockError = new DeprecatedMockError();
            // sofaResponse.setAppResponse(mockError);
            // 对sofaResponse进行序列化的hex string
            String encodeMsg
                    =
                    "4fbe636f6d2e616c697061792e736f66612e7270632e636f72652e726573706f6e73652e536f6661526573706f6e7365940769734572726f72086572726f724d73670b617070526573706f6e73650d726573706f6e736550726f70736f90464e4fc83e636f6d2e616c697061792e736f66612e7270632e636f6465632e736f66616865737369616e2e6d6f636b2e446570726563617465644d6f636b4572726f72940d64657461696c4d6573736167650563617573650a737461636b54726163651473757070726573736564457863657074696f6e736f914e4a015674001c5b6a6176612e6c616e672e537461636b5472616365456c656d656e746e1c4fab6a6176612e6c616e672e537461636b5472616365456c656d656e74940e6465636c6172696e67436c6173730a6d6574686f644e616d650866696c654e616d650a6c696e654e756d6265726f92530051636f6d2e616c697061792e736f66612e7270632e636f6465632e736f66616865737369616e2e73657269616c697a652e536f6661526573706f6e73654865737369616e53657269616c697a65725465737453003574657374437573746f6d5468726f7761626c65446573657269616c697a6572456e61626c6564466f72496e636f6d70617469626c65530026536f6661526573706f6e73654865737369616e53657269616c697a6572546573742e6a617661c86c6f9253002473756e2e7265666c6563742e4e61746976654d6574686f644163636573736f72496d706c07696e766f6b65301d4e61746976654d6574686f644163636573736f72496d706c2e6a6176618e6f9253002473756e2e7265666c6563742e4e61746976654d6574686f644163636573736f72496d706c06696e766f6b651d4e61746976654d6574686f644163636573736f72496d706c2e6a617661c83e6f9253002873756e2e7265666c6563742e44656c65676174696e674d6574686f644163636573736f72496d706c06696e766f6b6553002144656c65676174696e674d6574686f644163636573736f72496d706c2e6a617661bb6f92186a6176612e6c616e672e7265666c6563742e4d6574686f6406696e766f6b650b4d6574686f642e6a617661c9f26f925300296f72672e6a756e69742e72756e6e6572732e6d6f64656c2e4672616d65776f726b4d6574686f6424311172756e5265666c65637469766543616c6c144672616d65776f726b4d6574686f642e6a617661c83b6f925300336f72672e6a756e69742e696e7465726e616c2e72756e6e6572732e6d6f64656c2e5265666c65637469766543616c6c61626c650372756e175265666c65637469766543616c6c61626c652e6a6176619c6f925300276f72672e6a756e69742e72756e6e6572732e6d6f64656c2e4672616d65776f726b4d6574686f6411696e766f6b654578706c6f736976656c79144672616d65776f726b4d6574686f642e6a617661c8386f925300326f72672e6a756e69742e696e7465726e616c2e72756e6e6572732e73746174656d656e74732e496e766f6b654d6574686f64086576616c7561746511496e766f6b654d6574686f642e6a617661a16f925300206f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65722433086576616c7561746511506172656e7452756e6e65722e6a617661c9326f9253002a6f72672e6a756e69742e72756e6e6572732e426c6f636b4a556e697434436c61737352756e6e65722431086576616c756174651b426c6f636b4a556e697434436c61737352756e6e65722e6a617661c8646f921e6f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65720772756e4c65616611506172656e7452756e6e65722e6a617661c96e6f925300286f72672e6a756e69742e72756e6e6572732e426c6f636b4a556e697434436c61737352756e6e65720872756e4368696c641b426c6f636b4a556e697434436c61737352756e6e65722e6a617661c8676f925300286f72672e6a756e69742e72756e6e6572732e426c6f636b4a556e697434436c61737352756e6e65720872756e4368696c641b426c6f636b4a556e697434436c61737352756e6e65722e6a617661c83f6f925300206f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e657224340372756e11506172656e7452756e6e65722e6a617661c94b6f925300206f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65722431087363686564756c6511506172656e7452756e6e65722e6a617661c84f6f921e6f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65720b72756e4368696c6472656e11506172656e7452756e6e65722e6a617661c9496f921e6f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65720a6163636573732431303011506172656e7452756e6e65722e6a617661c8426f925300206f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65722432086576616c7561746511506172656e7452756e6e65722e6a617661c9256f925300206f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65722433086576616c7561746511506172656e7452756e6e65722e6a617661c9326f921e6f72672e6a756e69742e72756e6e6572732e506172656e7452756e6e65720372756e11506172656e7452756e6e65722e6a617661c99d6f921a6f72672e6a756e69742e72756e6e65722e4a556e6974436f72650372756e0e4a556e6974436f72652e6a617661c8896f92530028636f6d2e696e74656c6c696a2e6a756e6974342e4a556e697434496465615465737452756e6e657213737461727452756e6e65725769746841726773194a556e697434496465615465737452756e6e65722e6a617661c8456f9253002f636f6d2e696e74656c6c696a2e72742e6a756e69742e496465615465737452756e6e65722452657065617465722431076578656375746513496465615465737452756e6e65722e6a617661b66f9253002d636f6d2e696e74656c6c696a2e72742e657865637574696f6e2e6a756e69742e546573747352657065617465720672657065617412546573747352657065617465722e6a6176619b6f9253002d636f6d2e696e74656c6c696a2e72742e6a756e69742e496465615465737452756e6e657224526570656174657213737461727452756e6e6572576974684172677313496465615465737452756e6e65722e6a617661b36f92530022636f6d2e696e74656c6c696a2e72742e6a756e69742e4a556e697453746172746572167072657061726553747265616d73416e645374617274114a556e6974537461727465722e6a617661c8eb6f92530022636f6d2e696e74656c6c696a2e72742e6a756e69742e4a556e697453746172746572046d61696e114a556e6974537461727465722e6a617661c8367a567400326a6176612e7574696c2e436f6c6c656374696f6e7324556e6d6f6469666961626c6552616e646f6d4163636573734c6973746e007a4e";
            ByteArrayWrapperByteBuf bsIn = new ByteArrayWrapperByteBuf(hexToByte(encodeMsg));
            Map<String, String> ctx = new HashMap<>();
            ctx.put(RemotingConstants.HEAD_GENERIC_TYPE, "2");
            SofaResponse sofaResponse2 = new SofaResponse();
            serializer.decodeObjectByTemplate(bsIn, ctx, sofaResponse2);
            Assert.assertTrue(sofaResponse2.getAppResponse() instanceof RuntimeException);
            Assert.assertTrue(((RuntimeException) sofaResponse2.getAppResponse()).getMessage().startsWith(
                    "occur business exception, but type=com.alipay.sofa.rpc.codec.sofahessian.mock.DeprecatedMockError class is not "
                            + "found, error: "));
        } finally {
            setGenericThrowException(false);
        }
    }

    public static byte[] hexToByte(String hex) {
        int m = 0, n = 0;
        int byteLen = hex.length() / 2;
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte) intVal);
        }
        return ret;
    }

    public static String byteToHex(byte[] bytes) {
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
        }
        return sb.toString().trim();
    }
}