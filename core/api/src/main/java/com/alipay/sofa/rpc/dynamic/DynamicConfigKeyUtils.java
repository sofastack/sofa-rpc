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
 * @version $Id: DynamicConfigKeyUtils.java, v 0.1 2019年04月16日 下午12:01 lepdou Exp $
 */
public class DynamicConfigKeyUtils {

    private static final String KEY_SEPARATOR                        = ".";

    private static final String APP_CONSUMER_CONFIG_KEY_PREFIX       = "sofa.consumer";
    private static final String APP_PROVIDER_CONFIG_KEY_PREFIX       = "sofa.provider";
    private static final String SERVICE_CONSUMER_PROPERTY_KEY_PREFIX = "sofa.consumer.service";
    private static final String SERVICE_PROVIDER_PROPERTY_KEY_PREFIX = "sofa.provider.service";
    private static final String METHOD_CONSUMER_PROPERTY_KEY_PREFIX  = "sofa.consumer.method";
    private static final String METHOD_PROVIDER_PROPERTY_KEY_PREFIX  = "sofa.provider.method";

    public static String extractPropertyKey(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return key.substring(key.lastIndexOf(KEY_SEPARATOR) + 1);
    }

    /**
     * sofa.consumer.service.{serivceName}.{configKey}
     *
     * @param srcProKey sofa.consumer.service.com.alipay.sofa.rpc.test.HelloService.timeout
     * @return service.com.alipay.sofa.rpc.test.HelloService
     */
    public static String extractServiceNameFromServiceProKey(String srcProKey) {
        if (StringUtils.isBlank(srcProKey) || (!srcProKey.startsWith(SERVICE_CONSUMER_PROPERTY_KEY_PREFIX)
            && !srcProKey.startsWith(SERVICE_PROVIDER_PROPERTY_KEY_PREFIX))) {
            return "";
        }

        return srcProKey.substring(SERVICE_CONSUMER_PROPERTY_KEY_PREFIX.length() + 1,
            srcProKey.lastIndexOf(KEY_SEPARATOR));
    }

    /**
     * sofa.consumer.method.{serivceName}.{methodName}.{configKey}
     *
     * @param methodProKey sofa.consumer.method.com.alipay.sofa.rpc.test.HelloService.sayHello.timeout
     * @return com.alipay.sofa.rpc.test.HelloService
     */
    public static String extractServiceNameFromMethodProKey(String methodProKey) {
        if (StringUtils.isBlank(methodProKey) || (!methodProKey.startsWith(METHOD_CONSUMER_PROPERTY_KEY_PREFIX)
            && !methodProKey.startsWith(METHOD_PROVIDER_PROPERTY_KEY_PREFIX))) {
            return "";
        }

        String serviceMethod = methodProKey.substring(METHOD_PROVIDER_PROPERTY_KEY_PREFIX.length() + 1,
            methodProKey.lastIndexOf(KEY_SEPARATOR));

        return serviceMethod.substring(0, serviceMethod.lastIndexOf(KEY_SEPARATOR));
    }

    /**
     * sofa.consumer.method.{serivceName}.{methodName}.{configKey}={configValue}
     *
     * @param methodProKey sofa.consumer.method.com.alipay.sofa.rpc.test.HelloService.sayHello.timeout=1000
     * @return sayHello
     */
    public static String extractMethodNameFromMethodProKey(String methodProKey) {
        if (StringUtils.isBlank(methodProKey) || (!methodProKey.startsWith(METHOD_CONSUMER_PROPERTY_KEY_PREFIX)
            && !methodProKey.startsWith(METHOD_PROVIDER_PROPERTY_KEY_PREFIX))) {
            return "";
        }

        String serviceMethod = methodProKey.substring(METHOD_PROVIDER_PROPERTY_KEY_PREFIX.length() + 1,
            methodProKey.lastIndexOf(KEY_SEPARATOR));

        return serviceMethod.substring(serviceMethod.lastIndexOf(KEY_SEPARATOR) + 1);
    }

    /**
     * sofa.consumer.service.{serivceName}.{configKey}
     */
    public static String buildConsumerServiceProKey(String serviceName, String proKey) {
        return SERVICE_CONSUMER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + proKey;
    }

    /**
     * sofa.provider.service.{serivceName}.{configKey}
     */
    public static String buildProviderServiceProKey(String serviceName, String proKey) {
        return SERVICE_PROVIDER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + proKey;
    }

    /**
     * sofa.consumer.service.{serivceName}.{methodName}.{configKey}
     */
    public static String buildConsumerMethodProKey(String serviceName, String methodName, String proKey) {
        return SERVICE_CONSUMER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + methodName +
            KEY_SEPARATOR + proKey;
    }

    /**
     * sofa.provider.service.{serivceName}.{methodName}.{configKey}
     */
    public static String buildProviderMethodProKey(String serviceName, String methodName, String proKey) {
        return SERVICE_PROVIDER_PROPERTY_KEY_PREFIX + KEY_SEPARATOR + serviceName + KEY_SEPARATOR + methodName +
            KEY_SEPARATOR + proKey;
    }

}