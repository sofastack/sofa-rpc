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
package com.alipay.sofa.rpc.codec.quicklz;

import com.alipay.sofa.rpc.codec.Compressor;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

// QuickLZ data compression library
// Copyright (C) 2006-2011 Lasse Mikkel Reinhold
// lar@quicklz.com
//
// QuickLZ can be used for free under the GPL 1, 2 or 3 license (where anything
// released into public must be open source) or under a commercial license if such
// has been acquired (see http://www.quicklz.com/order.html). The commercial license
//
// does not cover derived or ported versions created by third parties under GPL.
// Only a subset of the C library has been ported, namely level 1 and 3 not in
// streaming mode.
//
// Version: 1.5.0 final
@Extension(value = "quicklz", code = 1)
public class QuickLZCompressor implements Compressor {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(QuickLZCompressor.class);

    @Override
    public byte[] compress(byte[] src) {
        return compress(src, 1);
    }

    @Override
    public byte[] deCompress(byte[] src) {
        return decompress(src);
    }

    public QuickLZCompressor() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Init LZMA compressor");
        }
    }

    // Streaming mode not supported
    public final static int  QLZ_STREAMING_BUFFER   = 0;

    // Bounds checking not supported. Use try...catch instead
    public final static int  QLZ_MEMORY_SAFE        = 0;

    public final static int  QLZ_VERSION_MAJOR      = 1;
    public final static int  QLZ_VERSION_MINOR      = 5;
    public final static int  QLZ_VERSION_REVISION   = 0;

    // Decrease QLZ_POINTERS_3 to increase compression speed of level 3. Do not
    // edit any other constants!
    private final static int HASH_VALUES            = 4096;
    private final static int MINOFFSET              = 2;
    private final static int UNCONDITIONAL_MATCHLEN = 6;
    private final static int UNCOMPRESSED_END       = 4;
    private final static int CWORD_LEN              = 4;
    private final static int DEFAULT_HEADERLEN      = 9;
    private final static int QLZ_POINTERS_1         = 1;
    private final static int QLZ_POINTERS_3         = 16;

    int headerLen(byte[] source) {
        return ((source[0] & 2) == 2) ? 9 : 3;
    }

    public long sizeDecompressed(byte[] source) {
        if (headerLen(source) == 9) {
            return fast_read(source, 5, 4);
        } else {
            return fast_read(source, 2, 1);
        }
    }

    public long sizeCompressed(byte[] source) {
        if (headerLen(source) == 9) {
            return fast_read(source, 1, 4);
        } else {
            return fast_read(source, 1, 1);
        }
    }

    private void write_header(byte[] dst, int level, boolean compressible, int size_compressed, int size_decompressed) {
        dst[0] = (byte) (2 | (compressible ? 1 : 0));
        dst[0] |= (byte) (level << 2);
        dst[0] |= (1 << 6);
        dst[0] |= (0 << 4);
        fast_write(dst, 1, size_decompressed, 4);
        fast_write(dst, 5, size_compressed, 4);
    }

    public byte[] compress(byte[] source, int level) {
        int src = 0;
        int dst = DEFAULT_HEADERLEN + CWORD_LEN;
        long cword_val = 0x80000000L;
        int cword_ptr = DEFAULT_HEADERLEN;
        byte[] destination = new byte[source.length + 400];
        int[][] hashtable;
        int[] cachetable = new int[HASH_VALUES];
        byte[] hash_counter = new byte[HASH_VALUES];
        byte[] d2;
        int fetch = 0;
        int last_matchstart = (source.length - UNCONDITIONAL_MATCHLEN - UNCOMPRESSED_END - 1);
        int lits = 0;

        if (level != 1 && level != 3) {
            throw new RuntimeException("Java version only supports level 1 and 3");
        }

        if (level == 1) {
            hashtable = new int[HASH_VALUES][QLZ_POINTERS_1];
        } else {
            hashtable = new int[HASH_VALUES][QLZ_POINTERS_3];
        }

        if (source.length == 0) {
            return new byte[0];
        }

        if (src <= last_matchstart) {
            fetch = (int) fast_read(source, src, 3);
        }

        while (src <= last_matchstart) {
            if ((cword_val & 1) == 1) {
                if (src > 3 * (source.length >> 2) && dst > src - (src >> 5)) {
                    d2 = new byte[source.length + DEFAULT_HEADERLEN];
                    write_header(d2, level, false, source.length, source.length + DEFAULT_HEADERLEN);
                    System.arraycopy(source, 0, d2, DEFAULT_HEADERLEN, source.length);
                    return d2;
                }

                fast_write(destination, cword_ptr, (cword_val >>> 1) | 0x80000000L, 4);
                cword_ptr = dst;
                dst += CWORD_LEN;
                cword_val = 0x80000000L;
            }

            if (level == 1) {
                int hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);
                int o = hashtable[hash][0];
                int cache = cachetable[hash] ^ fetch;

                cachetable[hash] = fetch;
                hashtable[hash][0] = src;

                if (cache == 0 &&
                    hash_counter[hash] != 0 &&
                    (src - o > MINOFFSET || (src == o + 1 && lits >= 3 && src > 3 && source[src] == source[src - 3] &&
                        source[src] == source[src - 2] && source[src] == source[src - 1] &&
                        source[src] == source[src + 1] && source[src] == source[src + 2]))) {
                    cword_val = ((cword_val >>> 1) | 0x80000000L);
                    if (source[o + 3] != source[src + 3]) {
                        int f = 3 - 2 | (hash << 4);
                        destination[dst + 0] = (byte) (f >>> 0 * 8);
                        destination[dst + 1] = (byte) (f >>> 1 * 8);
                        src += 3;
                        dst += 2;
                    } else {
                        int old_src = src;
                        int remaining = ((source.length - UNCOMPRESSED_END - src + 1 - 1) > 255 ? 255 : (source.length -
                            UNCOMPRESSED_END - src + 1 - 1));

                        src += 4;
                        if (source[o + src - old_src] == source[src]) {
                            src++;
                            if (source[o + src - old_src] == source[src]) {
                                src++;
                                while (source[o + (src - old_src)] == source[src] && (src - old_src) < remaining) {
                                    src++;
                                }
                            }
                        }

                        int matchlen = src - old_src;

                        hash <<= 4;
                        if (matchlen < 18) {
                            int f = hash | (matchlen - 2);
                            // Neither Java nor C# wants to inline fast_write
                            destination[dst + 0] = (byte) (f >>> 0 * 8);
                            destination[dst + 1] = (byte) (f >>> 1 * 8);
                            dst += 2;
                        } else {
                            int f = hash | (matchlen << 16);
                            fast_write(destination, dst, f, 3);
                            dst += 3;
                        }
                    }
                    lits = 0;
                    fetch = (int) fast_read(source, src, 3);
                } else {
                    lits++;
                    hash_counter[hash] = 1;
                    destination[dst] = source[src];
                    cword_val = (cword_val >>> 1);
                    src++;
                    dst++;
                    fetch = ((fetch >>> 8) & 0xffff) | ((((int) source[src + 2]) & 0xff) << 16);
                }
            } else {
                fetch = (int) fast_read(source, src, 3);

                int o, offset2;
                int matchlen, k, m = 0; //best_k = 0;
                byte c;
                int remaining = ((source.length - UNCOMPRESSED_END - src + 1 - 1) > 255 ? 255 : (source.length -
                    UNCOMPRESSED_END - src + 1 - 1));
                int hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);

                c = hash_counter[hash];
                matchlen = 0;
                offset2 = 0;
                for (k = 0; k < QLZ_POINTERS_3 && (c > k || c < 0); k++) {
                    o = hashtable[hash][k];
                    if ((byte) fetch == source[o] && (byte) (fetch >>> 8) == source[o + 1] &&
                        (byte) (fetch >>> 16) == source[o + 2] && o < src - MINOFFSET) {
                        m = 3;
                        while (source[o + m] == source[src + m] && m < remaining) {
                            m++;
                        }
                        if ((m > matchlen) || (m == matchlen && o > offset2)) {
                            offset2 = o;
                            matchlen = m;
                            //best_k = k;
                        }
                    }
                }
                o = offset2;
                hashtable[hash][c & (QLZ_POINTERS_3 - 1)] = src;
                c++;
                hash_counter[hash] = c;

                if (matchlen >= 3 && src - o < 131071) {
                    int offset = src - o;
                    for (int u = 1; u < matchlen; u++) {
                        fetch = (int) fast_read(source, src + u, 3);
                        hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);
                        c = hash_counter[hash]++;
                        hashtable[hash][c & (QLZ_POINTERS_3 - 1)] = src + u;
                    }

                    src += matchlen;
                    cword_val = ((cword_val >>> 1) | 0x80000000L);

                    if (matchlen == 3 && offset <= 63) {
                        fast_write(destination, dst, offset << 2, 1);
                        dst++;
                    } else if (matchlen == 3 && offset <= 16383) {
                        fast_write(destination, dst, (offset << 2) | 1, 2);
                        dst += 2;
                    } else if (matchlen <= 18 && offset <= 1023) {
                        fast_write(destination, dst, ((matchlen - 3) << 2) | (offset << 6) | 2, 2);
                        dst += 2;
                    } else if (matchlen <= 33) {
                        fast_write(destination, dst, ((matchlen - 2) << 2) | (offset << 7) | 3, 3);
                        dst += 3;
                    } else {
                        fast_write(destination, dst, ((matchlen - 3) << 7) | (offset << 15) | 3, 4);
                        dst += 4;
                    }
                } else {
                    destination[dst] = source[src];
                    cword_val = (cword_val >>> 1);
                    src++;
                    dst++;
                }
            }
        }

        while (src <= source.length - 1) {
            if ((cword_val & 1) == 1) {
                fast_write(destination, cword_ptr, (long) ((cword_val >>> 1) | 0x80000000L), 4);
                cword_ptr = dst;
                dst += CWORD_LEN;
                cword_val = 0x80000000L;
            }

            destination[dst] = source[src];
            src++;
            dst++;
            cword_val = (cword_val >>> 1);
        }
        while ((cword_val & 1) != 1) {
            cword_val = (cword_val >>> 1);
        }
        fast_write(destination, cword_ptr, (long) ((cword_val >>> 1) | 0x80000000L), CWORD_LEN);
        write_header(destination, level, true, source.length, dst);

        d2 = new byte[dst];
        System.arraycopy(destination, 0, d2, 0, dst);
        return d2;
    }

    long fast_read(byte[] a, int i, int numbytes) {
        long l = 0;
        for (int j = 0; j < numbytes; j++) {
            l |= ((((int) a[i + j]) & 0xffL) << j * 8);
        }
        return l;
    }

    void fast_write(byte[] a, int i, long value, int numbytes) {
        for (int j = 0; j < numbytes; j++) {
            a[i + j] = (byte) (value >>> (j * 8));
        }
    }

    public byte[] decompress(byte[] source) {
        int size = (int) sizeDecompressed(source);
        int src = headerLen(source);
        int dst = 0;
        long cword_val = 1;
        byte[] destination = new byte[size];
        int[] hashtable = new int[4096];
        byte[] hash_counter = new byte[4096];
        int last_matchstart = size - UNCONDITIONAL_MATCHLEN - UNCOMPRESSED_END - 1;
        int last_hashed = -1;
        int hash;
        int fetch = 0;

        int level = (source[0] >>> 2) & 0x3;

        if (level != 1 && level != 3) {
            throw new RuntimeException("Java version only supports level 1 and 3");
        }

        if ((source[0] & 1) != 1) {
            byte[] d2 = new byte[size];
            System.arraycopy(source, headerLen(source), d2, 0, size);
            return d2;
        }
        for (;;) {
            if (cword_val == 1) {
                cword_val = fast_read(source, src, 4);
                src += 4;
                if (dst <= last_matchstart) {
                    if (level == 1) {
                        fetch = (int) fast_read(source, src, 3);
                    } else {
                        fetch = (int) fast_read(source, src, 4);
                    }
                }
            }

            if ((cword_val & 1) == 1) {
                int matchlen;
                int offset2;

                cword_val = cword_val >>> 1;

                if (level == 1) {
                    hash = (fetch >>> 4) & 0xfff;
                    offset2 = hashtable[hash];

                    if ((fetch & 0xf) != 0) {
                        matchlen = (fetch & 0xf) + 2;
                        src += 2;
                    } else {
                        matchlen = ((int) source[src + 2]) & 0xff;
                        src += 3;
                    }
                } else {
                    int offset;

                    if ((fetch & 3) == 0) {
                        offset = (fetch & 0xff) >>> 2;
                        matchlen = 3;
                        src++;
                    } else if ((fetch & 2) == 0) {
                        offset = (fetch & 0xffff) >>> 2;
                        matchlen = 3;
                        src += 2;
                    } else if ((fetch & 1) == 0) {
                        offset = (fetch & 0xffff) >>> 6;
                        matchlen = ((fetch >>> 2) & 15) + 3;
                        src += 2;
                    } else if ((fetch & 127) != 3) {
                        offset = (fetch >>> 7) & 0x1ffff;
                        matchlen = ((fetch >>> 2) & 0x1f) + 2;
                        src += 3;
                    } else {
                        offset = (fetch >>> 15);
                        matchlen = ((fetch >>> 7) & 255) + 3;
                        src += 4;
                    }
                    offset2 = (int) (dst - offset);
                }

                destination[dst + 0] = destination[offset2 + 0];
                destination[dst + 1] = destination[offset2 + 1];
                destination[dst + 2] = destination[offset2 + 2];

                for (int i = 3; i < matchlen; i += 1) {
                    destination[dst + i] = destination[offset2 + i];
                }
                dst += matchlen;

                if (level == 1) {
                    fetch = (int) fast_read(destination, last_hashed + 1, 3); // destination[last_hashed + 1] | (destination[last_hashed + 2] << 8) | (destination[last_hashed + 3] << 16);
                    while (last_hashed < dst - matchlen) {
                        last_hashed++;
                        hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);
                        hashtable[hash] = last_hashed;
                        hash_counter[hash] = 1;
                        fetch = fetch >>> 8 & 0xffff | (((int) destination[last_hashed + 3]) & 0xff) << 16;
                    }
                    fetch = (int) fast_read(source, src, 3);
                } else {
                    fetch = (int) fast_read(source, src, 4);
                }
                last_hashed = dst - 1;
            } else {
                if (dst <= last_matchstart) {
                    destination[dst] = source[src];
                    dst += 1;
                    src += 1;
                    cword_val = cword_val >>> 1;

                    if (level == 1) {
                        while (last_hashed < dst - 3) {
                            last_hashed++;
                            int fetch2 = (int) fast_read(destination, last_hashed, 3);
                            hash = ((fetch2 >>> 12) ^ fetch2) & (HASH_VALUES - 1);
                            hashtable[hash] = last_hashed;
                            hash_counter[hash] = 1;
                        }
                        fetch = fetch >> 8 & 0xffff | (((int) source[src + 2]) & 0xff) << 16;
                    } else {
                        fetch = fetch >> 8 & 0xffff | (((int) source[src + 2]) & 0xff) << 16 |
                            (((int) source[src + 3]) & 0xff) << 24;
                    }
                } else {
                    while (dst <= size - 1) {
                        if (cword_val == 1) {
                            src += CWORD_LEN;
                            cword_val = 0x80000000L;
                        }

                        destination[dst] = source[src];
                        dst++;
                        src++;
                        cword_val = cword_val >>> 1;
                    }
                    return destination;
                }
            }
        }
    }
}
