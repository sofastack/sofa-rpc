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
package com.alipay.sofa.rpc.doc.swagger.utils;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class LocalVariableTableParameterNameDiscovererTest {

    private final static Logger                                    LOGGER     = LoggerFactory
                                                                                  .getLogger(LocalVariableTableParameterNameDiscoverer.class);

    private final static LocalVariableTableParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    private static Method                                          CLASS_NO_PARAM_METHOD;
    private static Method                                          CLASS_ONE_PARAM_METHOD;
    private static Method                                          CLASS_MULTI_PARAM_METHOD;

    static {
        try {
            CLASS_NO_PARAM_METHOD = DemoClass.class.getMethod("noParam");
            CLASS_ONE_PARAM_METHOD = DemoClass.class.getMethod("oneParam", Object.class);
            CLASS_MULTI_PARAM_METHOD = DemoClass.class.getMethod("multiParam", Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("fail to get method of DemoClass", e);
        }
    }

    @Test
    public void test() {
        String[] parameterNames;

        parameterNames = DISCOVERER.getParameterNames(CLASS_NO_PARAM_METHOD);
        Assert.assertTrue(CommonUtils.isEmpty(parameterNames));

        parameterNames = DISCOVERER.getParameterNames(CLASS_ONE_PARAM_METHOD);
        Assert.assertEquals("a", parameterNames[0]);

        parameterNames = DISCOVERER.getParameterNames(CLASS_MULTI_PARAM_METHOD);
        Assert.assertEquals("a", parameterNames[0]);
        Assert.assertEquals("b", parameterNames[1]);
    }
}