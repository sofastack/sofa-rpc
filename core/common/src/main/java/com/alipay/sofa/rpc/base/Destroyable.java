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
package com.alipay.sofa.rpc.base;

/**
 * <p>可销毁的接口</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public interface Destroyable {

    /**
     * 销毁接口
     */
    public void destroy();

    /**
     * Do destroy with hook.
     * 
     * @param hook DestroyHook
     */
    public void destroy(DestroyHook hook);

    /**
     * 销毁钩子
     */
    interface DestroyHook {
        /**
         * 销毁前要做的事情
         */
        public void preDestroy();

        /**
         * 銷毀后要做的事情
         */
        public void postDestroy();
    }
}
