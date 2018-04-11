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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.ext.Extension;

import static com.alipay.sofa.rpc.module.TestModules.error;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("test2")
public class TestLoadModule2 implements Module {
    @Override
    public boolean needLoad() {
        return true;
    }

    @Override
    public void install() {
        TestModules.test2 = "test2i";
    }

    @Override
    public void uninstall() {
        if (error) {
            throw new RuntimeException("uninstall test error");
        }
        TestModules.test2 = "test2u";
    }
}
