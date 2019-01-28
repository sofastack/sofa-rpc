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
package com.alipay.sofa.rpc.ext.proxy;

import com.alipay.sofa.rpc.ext.Extension;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "test3", proxy = true)
public class TestExtension3 implements TestExtensible {

    private String prefix;

    public TestExtension3(String prefix) {
        this.prefix = prefix;
    }

    private TestExtensible proxy;

    public void setProxy(TestExtensible proxy) {
        this.proxy = proxy;
    }

    @Override
    public String echo(String name) {
        return "test3" + proxy.echo(name);
    }
}
