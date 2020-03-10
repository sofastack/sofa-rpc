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
package com.alipay.sofa.rpc.hystrix;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class DefaultSetterFactoryTest {

    @Test
    public void testGenerateCommandKey() {
        for (Method method : TestCase.class.getMethods()) {
            if (method.isAnnotationPresent(HystrixCommandKey.class)) {
                HystrixCommandKey annotation = method.getAnnotation(HystrixCommandKey.class);
                Assert.assertEquals(annotation.value(), DefaultSetterFactory.generateCommandKey("TestCase", method));
            }
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @interface HystrixCommandKey {

        String value();
    }

    public interface TestCase {

        @HystrixCommandKey("TestCase#test1(String,int)")
        String test1(String param1, int param2);

        @HystrixCommandKey("TestCase#test2(String)")
        String test2(String param1);

        @HystrixCommandKey("TestCase#test3(String,Integer)")
        String test3(String param1, Integer param2);

        @HystrixCommandKey("TestCase#test4()")
        String test4();

        @HystrixCommandKey("TestCase#test5(List,Map)")
        String test5(List<String> param1, Map<String, String> param2);

        @HystrixCommandKey("TestCase#test6(Model)")
        String test6(Model param1);

        @HystrixCommandKey("TestCase#test7(String[],int[])")
        String test7(String[] param1, int[] param2);

        class Model {

        }
    }
}