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
package com.alipay.sofa.rpc.test.generic;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.generic.bean.BasicBean;
import com.alipay.sofa.rpc.test.generic.bean.ComplexBean;
import com.alipay.sofa.rpc.test.generic.bean.People;

/**
 * @author xuanbei
 * @since 2016/07/28
 */
public class TestClass implements TestInterface {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestClass.class);

    @Override
    public People hello(People people) {
        return people;
    }

    @Override
    public People helloTimeout(People people) {
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return people;
    }

    @Override
    public People helloCallbackException(People people) {
        throw new RuntimeException("Hello~");
    }

    @Override
    public People helloCallback(People people) {
        return people;
    }

    @Override
    public People helloFuture(People people) {
        return people;
    }

    @Override
    public void helloOneway(People people) {
        LOGGER.info("Hello, oneway! " + people.getName());
    }

    @Override
    public ComplexBean helloComplexBean(ComplexBean complexBean) {
        return complexBean;
    }

    @Override
    public BasicBean helloBasicBean(BasicBean basicBean) {
        return basicBean;
    }
}
