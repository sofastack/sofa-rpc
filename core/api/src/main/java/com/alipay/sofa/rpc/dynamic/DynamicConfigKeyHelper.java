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
package com.alipay.sofa.rpc.dynamic;

import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 *
 * @author lepdou
 * @version $Id: DynamicConfigKeyHelper.java, v 0.1 2019年04月16日 下午12:01 lepdou Exp $
 */
public class DynamicConfigKeyHelper {

    private static final String KEY_SEPARATOR                        = ".";

    public static final String  APP_CONSUMER_CONFIG_KEY_PREFIX       = "sofa.consumer";
    public static final String  APP_PROVIDER_CONFIG_KEY_PREFIX       = "sofa.provider";
    public static final String  SERVICE_CONSUMER_PROPERTY_KEY_PREFIX = "sofa.consumer.service";
    public static final String  SERVICE_PROVIDER_PROPERTY_KEY_PREFIX = "sofa.provider.service";
    public static final String  METHOD_CONSUMER_PROPERTY_KEY_PREFIX  = "sofa.consumer.method";
    public static final String  METHOD_PROVIDER_PROPERTY_KEY_PREFIX  = "sofa.provider.method";

    /**
     * The last field of key is actual property key
     */
    public static String extractPropertyKey(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return key.substring(key.lastIndexOf(KEY_SEPARATOR) + 1);
    }

    /**
     * Service property key format : sofa.consumer.service.{serivceName}.{configKey}
     *
     * For example : sofa.consumer.service.com.alipay.sofa.rpc.test.HelloService.timeout
     * service name = com.alipay.sofa.rpc.test.HelloService
     * config key   = timeout
     *
     * @param serviceProKey service property key
     * @return extracted service name
     */
    public static String extractServiceNameFromServiceProKey(String serviceProKey) {
        if (!isServiceProKey(serviceProKey)) {
            return "";
        }

        return serviceProKey.substring(SERVICE_CONSUMER_PROPERTY_KEY_PREFIX.length() + 1,
            serviceProKey.lastIndexOf(KEY_SEPARATOR));
    }

    /**
     * Method property key format : sofa.consumer.method.{serivceName}.{methodName}.{configKey}
     *
     * Example : sofa.consumer.method.com.alipay.sofa.rpc.test.HelloService.sayHello.timeout
     * service name = com.alipay.sofa.rpc.test.HelloService
     * method name  = sayHello
     * config key   = timeout
     *
     * @param methodProKey method property key
     * @return extracted service name
     */
    public static String extractServiceNameFromMethodProKey(String methodProKey) {
        if (!isMethodProKey(methodProKey)) {
            return "";
        }

        String serviceMethod = methodProKey.substring(METHOD_PROVIDER_PROPERTY_KEY_PREFIX.length() + 1,
            methodProKey.lastIndexOf(KEY_SEPARATOR));

        return serviceMethod.substring(0, serviceMethod.lastIndexOf(KEY_SEPARATOR));
    }

    /**
     * Method property key format : sofa.consumer.method.{serivceName}.{methodName}.{configKey}
     *
     * Example : sofa.consumer.method.com.alipay.sofa.rpc.test.HelloService.sayHello.timeout
     * service name = com.alipay.sofa.rpc.test.HelloService
     * method name  = sayHello
     * config key   = timeout
     *
     * @param methodProKey method property key
     * @return extracted method name
     */
    public static String extractMethodNameFromMethodProKey(String methodProKey) {
        if (!isMethodProKey(methodProKey)) {
            return "";
        }

        String serviceMethod = methodProKey.substring(METHOD_PROVIDER_PROPERTY_KEY_PREFIX.length() + 1,
            methodProKey.lastIndexOf(KEY_SEPARATOR));

        return serviceMethod.substring(serviceMethod.lastIndexOf(KEY_SEPARATOR) + 1);
    }

    /**
     * Consumer service property key format : sofa.consumer.service.{serviceName}.{configKey}
     */
    public static String buildConsumerServiceProKey(String serviceName, String proKey) {
        return SERVICE_CONSUMER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + proKey;
    }

    /**
     * Provider service property key format : sofa.provider.service.{serviceName}.{configKey}
     */
    public static String buildProviderServiceProKey(String serviceName, String proKey) {
        return SERVICE_PROVIDER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + proKey;
    }

    /**
     * Consumer method property key format : sofa.consumer.service.{serviceName}.{methodName}.{configKey}
     */
    public static String buildConsumerMethodProKey(String serviceName, String methodName, String proKey) {
        return METHOD_CONSUMER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + methodName +
            KEY_SEPARATOR + proKey;
    }

    /**
     * Provider method property key format : sofa.consumer.service.{serviceName}.{methodName}.{configKey}
     */
    public static String buildProviderMethodProKey(String serviceName, String methodName, String proKey) {
        return METHOD_PROVIDER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + methodName +
            KEY_SEPARATOR + proKey;
    }

    public static boolean isServiceProKey(String key) {
        return !StringUtils.isBlank(key) && (key.startsWith(SERVICE_CONSUMER_PROPERTY_KEY_PREFIX) ||
            key.startsWith(SERVICE_PROVIDER_PROPERTY_KEY_PREFIX));
    }

    public static boolean isMethodProKey(String key) {
        return !StringUtils.isBlank(key) && (key.startsWith(METHOD_CONSUMER_PROPERTY_KEY_PREFIX) ||
            key.startsWith(METHOD_PROVIDER_PROPERTY_KEY_PREFIX));
    }

    public static boolean isSofaProKey(String key) {
        return isServiceProKey(key) || isMethodProKey(key);
    }
}