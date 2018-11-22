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
package com.alipay.sofa.rpc.common.struct;

/**
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface Cache {
    /**
     * 通过调用参数获得唯一的key，返回null以后将不从cache中load
     *
     * @param interfaceId 接口名
     * @param methodName  方法名
     * @param args        方法参数
     * @return 关键字，可以返回null
     */
    Object buildKey(String interfaceId, String methodName, Object[] args);

    /**
     * 放入缓存
     *
     * @param key    方法参数得到的关键字
     * @param result 缓存的调用结果
     */
    void put(Object key, Object result);

    /**
     * 拿出缓存
     *
     * @param key 方法参数得到的关键字
     * @return 缓存的调用结果
     */
    Object get(Object key);
}
