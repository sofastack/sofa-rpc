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
package com.alipay.sofa.rpc.transport;

import com.alipay.sofa.rpc.common.annotation.Unstable;

/**
 * <p>ByteBuf的一个抽象，这样可以隔离各种Bytebuf</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public abstract class AbstractByteBuf {

    /**
     * Get byte[] data
     *
     * @return byte[]
     */
    public abstract byte[] array();

    /**
     * Get length of readable bytes
     *
     * @return length
     */
    public abstract int readableBytes();

    /**
     * release byte buffer
     *
     * @return result
     */
    public abstract boolean release();
}
