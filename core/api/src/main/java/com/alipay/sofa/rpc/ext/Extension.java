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
package com.alipay.sofa.rpc.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展点
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @see Extensible
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Extension {
    /**
     * 扩展点名字
     *
     * @return 扩展点名字
     */
    String value();

    /**
     * 扩展点编码，默认不需要，当接口需要编码的时候需要
     *
     * @return 扩展点编码
     * @see Extensible#coded()
     */
    byte code() default -1;

    /**
     * 优先级排序，默认不需要
     *
     * @return 排序
     */
    int order() default 0;

    /**
     * 是否覆盖其它低{@link #order()}的同名扩展
     *
     * @return 是否覆盖其它低排序的同名扩展
     * @since 5.2.0
     */
    boolean override() default false;

    /**
     * 排斥其它扩展，可以排斥掉其它低{@link #order()}的扩展
     *
     * @return 排斥其它扩展
     * @since 5.2.0
     */
    String[] rejection() default {};
}
