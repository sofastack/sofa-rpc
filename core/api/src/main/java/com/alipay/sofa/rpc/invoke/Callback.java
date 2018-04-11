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
package com.alipay.sofa.rpc.invoke;

import com.alipay.sofa.rpc.common.annotation.Unstable;

import java.io.Closeable;

/**
 * <p>面向用户的，回调的抽象类，实现通知方法，指定传递对象</p>
 * <p>
 *
 * @param <Q> the request parameter
 * @param <S> the response parameter
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public interface Callback<Q, S> extends Closeable {

    /**
     * 回调通知
     *
     * @param result 通知对象
     * @return 返回值对象 s
     */
    S notify(Q result);
}
