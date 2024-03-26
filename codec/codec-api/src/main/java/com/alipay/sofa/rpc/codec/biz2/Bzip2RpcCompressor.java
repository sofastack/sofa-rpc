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
package com.alipay.sofa.rpc.codec.biz2;

import com.alipay.sofa.rpc.codec.Compressor;
import com.alipay.sofa.rpc.ext.Extension;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * bzip2 compressor, faster compression efficiency
 *
 * @author chengming
 * @version Bzip2RpcCompressor.java, v 0.1 2024年02月28日 10:45 AM chengming
 * @link https://commons.apache.org/proper/commons-compress/
 */
@Extension(value = "bzip2", code = 3)
public class Bzip2RpcCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] src) {
        if (null == src || 0 == src.length) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BZip2CompressorOutputStream cos;
        try {
            cos = new BZip2CompressorOutputStream(out);
            cos.write(src);
            cos.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] deCompress(byte[] src) {
        if (null == src || 0 == src.length) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        try {
            BZip2CompressorInputStream unZip = new BZip2CompressorInputStream(in);
            byte[] buffer = new byte[2048];
            int n;
            while ((n = unZip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }
}