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
package com.alipay.sofa.rpc.codec.gzip;

import com.alipay.sofa.rpc.codec.Compressor;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * @author chengming
 * @version GzipRpcCompressorTest.java, v 0.1 2024年02月28日 2:09 PM chengming
 */
public class GzipRpcCompressorTest {

    private static final String TEST_STR;

    static {
        StringBuilder builder = new StringBuilder();
        int charNum = 1000000;
        for (int i = 0; i < charNum; i++) {
            builder.append("a");
        }

        TEST_STR = builder.toString();
    }

    @Test
    public void testCompression() throws UnsupportedEncodingException {
        Compressor compressor = ExtensionLoaderFactory.getExtensionLoader(Compressor.class).getExtension("gzip");
        Assert.assertTrue(compressor instanceof GzipRpcCompressor);

        byte[] bs = compressor.compress(TEST_STR.getBytes("utf-8"));
        String s1 = new String(compressor.deCompress(bs), "utf-8");
        Assert.assertEquals(TEST_STR, s1);
    }

}
