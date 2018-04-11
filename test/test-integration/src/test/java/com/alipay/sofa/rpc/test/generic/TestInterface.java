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

import com.alipay.sofa.rpc.test.generic.bean.BasicBean;
import com.alipay.sofa.rpc.test.generic.bean.ComplexBean;
import com.alipay.sofa.rpc.test.generic.bean.People;

/**
 * @author xuanbei
 * @since 2016/07/28
 */
public interface TestInterface {
    People hello(People people);

    People helloTimeout(People people);

    People helloCallbackException(People people);

    People helloCallback(People people);

    People helloFuture(People people);

    void helloOneway(People people);

    ComplexBean helloComplexBean(ComplexBean complexBean);

    BasicBean helloBasicBean(BasicBean basicBean);
}
