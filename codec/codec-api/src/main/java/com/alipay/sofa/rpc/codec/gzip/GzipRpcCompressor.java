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
import com.alipay.sofa.rpc.ext.Extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author chengming
 * @version GzipRpcCompressor.java, v 0.1 2024年02月28日 11:25 AM chengming
 */
@Extension(value = "gzip", code = 4)
public class GzipRpcCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] src) {
        if (null == src || 0 == src.length) {
            return new byte[0];
        }

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteOutStream)) {
            gzipOutputStream.write(src);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        return byteOutStream.toByteArray();
    }

    @Override
    public byte[] deCompress(byte[] src) {
        if (null == src || 0 == src.length) {
            return new byte[0];
        }

        ByteArrayInputStream byteInStream = new ByteArrayInputStream(src);
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteInStream)) {
            int readByteNum;
            byte[] bufferArr = new byte[256];
            while ((readByteNum = gzipInputStream.read(bufferArr)) >= 0) {
                byteOutStream.write(bufferArr, 0, readByteNum);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        return byteOutStream.toByteArray();
    }
}