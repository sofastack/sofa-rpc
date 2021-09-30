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
package com.alipay.sofa.rpc.invoke.genericJson;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Date;

public class IGenericServiceImpl implements IGenericService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IGenericServiceImpl.class);

    @Override
    public TestEntity echoEntity(TestEntity testEntity) {
        LOGGER.info("receive entity: {}", testEntity);
        return testEntity;
    }

    @Override
    public String echoStr(String name) {
        LOGGER.info("receive name: {}", name);
        return name;
    }

    @Override
    public Integer echoInt(Integer age) {
        LOGGER.info("receive age: {}", age);
        return age;
    }

    @Override
    public Date echoDate(Date birth) {
        LOGGER.info("receive birth: {}", birth);
        return birth;
    }

    @Override
    public String testVoidParam() {
        LOGGER.info("receive void param");
        return "testVoid success!";
    }

    @Override
    public void testVoidResp() {
        LOGGER.info("server test void resp success!!!");
    }
}
