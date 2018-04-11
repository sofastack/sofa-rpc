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
package com.alipay.sofa.rpc.codec.snappy;

import com.alipay.sofa.rpc.codec.Compressor;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Arrays;

/**
 * SnappyRpcCompressor
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension(value = "snappy", code = 2)
public final class SnappyRpcCompressor implements Compressor {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SnappyRpcCompressor.class);

    public SnappyRpcCompressor() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Init Snappy compressor");
        }
    }

    @Override
    public byte[] deCompress(byte[] src) {
        return uncompress(src, 0, src.length);
    }

    public int getUncompressedLength(byte[] compressed, int compressedOffset)
        throws CorruptionException {
        return SnappyDecompressor.getUncompressedLength(compressed, compressedOffset);
    }

    public byte[] uncompress(byte[] compressed, int compressedOffset, int compressedSize)
        throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize);
    }

    public int uncompress(byte[] compressed, int compressedOffset, int compressedSize, byte[] uncompressed,
                          int uncompressedOffset)
        throws CorruptionException {
        return SnappyDecompressor.uncompress(compressed, compressedOffset, compressedSize, uncompressed,
            uncompressedOffset);
    }

    public int maxCompressedLength(int sourceLength) {
        return SnappyCompressor.maxCompressedLength(sourceLength);
    }

    public int compress(
                        byte[] uncompressed,
                        int uncompressedOffset,
                        int uncompressedLength,
                        byte[] compressed,
                        int compressedOffset) {
        return SnappyCompressor.compress(uncompressed,
            uncompressedOffset,
            uncompressedLength,
            compressed,
            compressedOffset);
    }

    @Override
    public byte[] compress(byte[] data) {
        byte[] compressedOut = new byte[maxCompressedLength(data.length)];
        int compressedSize = compress(data, 0, data.length, compressedOut, 0);
        byte[] trimmedBuffer = Arrays.copyOf(compressedOut, compressedSize);
        return trimmedBuffer;
    }

    static final int LITERAL            = 0;
    static final int COPY_1_BYTE_OFFSET = 1; // 3 bit length + 3 bits of offset in opcode
    static final int COPY_2_BYTE_OFFSET = 2;
    static final int COPY_4_BYTE_OFFSET = 3;
}
