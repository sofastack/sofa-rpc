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
 * <p>代表这个类或者接口是可扩展的，默认单例、不需要编码</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 * @see Extension
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Extensible {

    /**
     * 指定自定义扩展文件名称，默认就是全类名
     *
     * @return 自定义扩展文件名称
     */
    String file() default "";

    /**
     * 扩展类是否使用单例，默认使用
     *
     * @return 是否使用单例
     */
    boolean singleton() default true;

    /**
     * 扩展类是否需要编码，默认不需要
     *
     * @return 是否需要编码
     */
    boolean coded() default false;
}
