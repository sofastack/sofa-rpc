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
package com.alipay.sofa.rpc.log;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MiddlewareLoggerImplTest {

    @Test
    public void getName() throws Exception {
        Logger loggerFromClass = new MiddlewareLoggerImpl(MiddlewareLoggerImplTest.class);
        Logger logger = new MiddlewareLoggerImpl(MiddlewareLoggerImplTest.class.getCanonicalName());
        Assert.assertEquals(loggerFromClass.getName(), logger.getName());
        String appName = "app";
        if (logger.isDebugEnabled()) {
            logger.debug("debug");
            logger.debug("debug {}", "1");
            logger.debug("debug {} {} {}", "1", "2", "3");
            logger.debug("debug", new RuntimeException("runtime"));
        }
        if (logger.isDebugEnabled(appName)) {
            logger.debugWithApp(appName, "debug");
            logger.debugWithApp(appName, "debug {}", "1");
            logger.debugWithApp(appName, "debug {} {} {}", "1", "2", "3");
            logger.debugWithApp(appName, "debug", new RuntimeException("runtime"));
        }

        if (logger.isInfoEnabled()) {
            logger.info("info");
            logger.info("info {}", "1");
            logger.info("info {} {} {}", "1", "2", "3");
            logger.info("info", new RuntimeException("runtime"));
        }
        if (logger.isInfoEnabled(appName)) {
            logger.infoWithApp(appName, "info");
            logger.infoWithApp(appName, "info {}", "1");
            logger.infoWithApp(appName, "info {} {} {}", "1", "2", "3");
            logger.infoWithApp(appName, "info", new RuntimeException("runtime"));
        }

        if (logger.isWarnEnabled()) {
            logger.warn("warn");
            logger.warn("warn {}", "1");
            logger.warn("warn {} {} {}", "1", "2", "3");
            logger.warn("warn", new RuntimeException("runtime"));
        }
        if (logger.isWarnEnabled(appName)) {
            logger.warn(appName, "warn");
            logger.warnWithApp(appName, "warn {}", "1");
            logger.warnWithApp(appName, "warn {} {} {}", "1", "2", "3");
            logger.warnWithApp(appName, "warn", new RuntimeException("runtime"));
        }

        if (logger.isErrorEnabled()) {
            logger.error("error");
            logger.error("error {}", "1");
            logger.error("error {} {} {}", "1", "2", "3");
            logger.error("error", new RuntimeException("runtime"));
        }
        if (logger.isErrorEnabled(appName)) {
            logger.errorWithApp(appName, "error");
            logger.errorWithApp(appName, "error {}", "1");
            logger.errorWithApp(appName, "error {} {} {}", "1", "2", "3");
            logger.errorWithApp(appName, "error", new RuntimeException("runtime"));
        }
    }

}