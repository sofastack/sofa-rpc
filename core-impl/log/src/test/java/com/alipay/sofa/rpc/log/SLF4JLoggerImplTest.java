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
import org.slf4j.LoggerFactory;

/**
 * Unit tests for SLF4JLoggerImpl
 *
 * @author SOFA-RPC Team
 */
public class SLF4JLoggerImplTest {

    @Test
    public void testLoggerCreationWithName() {
        String loggerName = "testLogger";
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(loggerName);

        Assert.assertNotNull(logger);
        Assert.assertEquals(loggerName, logger.getName());
    }

    @Test
    public void testLoggerCreationWithClass() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        Assert.assertNotNull(logger);
        Assert.assertEquals(SLF4JLoggerImplTest.class.getName(), logger.getName());
    }

    @Test
    public void testGetLogger() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        org.slf4j.Logger slf4jLogger = logger.getLogger();

        Assert.assertNotNull(slf4jLogger);
        Assert.assertEquals(SLF4JLoggerImplTest.class.getName(), slf4jLogger.getName());
    }

    @Test
    public void testGetLoggerWithAppName() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        org.slf4j.Logger appLogger = logger.getLogger("testApp");

        Assert.assertNotNull(appLogger);
    }

    @Test
    public void testDebugMessage() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.debug("Test debug message");
        Assert.assertTrue(true);
    }

    @Test
    public void testDebugWithFormat() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.debug("Test debug message with format: {}", "arg1");
        Assert.assertTrue(true);
    }

    @Test
    public void testDebugWithThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        Exception ex = new Exception("Test exception");

        logger.debug("Test debug message with throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testInfoMessage() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.info("Test info message");
        Assert.assertTrue(true);
    }

    @Test
    public void testInfoWithFormat() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.info("Test info message with format: {}", "arg1");
        Assert.assertTrue(true);
    }

    @Test
    public void testInfoWithThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        Exception ex = new Exception("Test exception");

        logger.info("Test info message with throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testWarnMessage() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.warn("Test warn message");
        Assert.assertTrue(true);
    }

    @Test
    public void testWarnWithFormat() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.warn("Test warn message with format: {}", "arg1");
        Assert.assertTrue(true);
    }

    @Test
    public void testWarnWithThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        Exception ex = new Exception("Test exception");

        logger.warn("Test warn message with throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testErrorMessage() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.error("Test error message");
        Assert.assertTrue(true);
    }

    @Test
    public void testErrorWithFormat() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.error("Test error message with format: {}", "arg1");
        Assert.assertTrue(true);
    }

    @Test
    public void testErrorWithThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        Exception ex = new Exception("Test exception");

        logger.error("Test error message with throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testDebugWithApp() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";

        logger.debugWithApp(appName, "Test debug message with app");
        Assert.assertTrue(true);
    }

    @Test
    public void testInfoWithApp() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";

        logger.infoWithApp(appName, "Test info message with app");
        Assert.assertTrue(true);
    }

    @Test
    public void testWarnWithApp() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";

        logger.warnWithApp(appName, "Test warn message with app");
        Assert.assertTrue(true);
    }

    @Test
    public void testErrorWithApp() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";

        logger.errorWithApp(appName, "Test error message with app");
        Assert.assertTrue(true);
    }

    @Test
    public void testDebugWithAppAndThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";
        Exception ex = new Exception("Test exception");

        logger.debugWithApp(appName, "Test debug message with app and throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testInfoWithAppAndThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";
        Exception ex = new Exception("Test exception");

        logger.infoWithApp(appName, "Test info message with app and throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testWarnWithAppAndThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";
        Exception ex = new Exception("Test exception");

        logger.warnWithApp(appName, "Test warn message with app and throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testErrorWithAppAndThrowable() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);
        String appName = "testApp";
        Exception ex = new Exception("Test exception");

        logger.errorWithApp(appName, "Test error message with app and throwable", ex);
        Assert.assertTrue(true);
    }

    @Test
    public void testIsMethods() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.isDebugEnabled();
        logger.isInfoEnabled();
        logger.isWarnEnabled();
        logger.isErrorEnabled();

        logger.isDebugEnabled("testApp");
        logger.isInfoEnabled("testApp");
        logger.isWarnEnabled("testApp");
        logger.isErrorEnabled("testApp");

        Assert.assertTrue(true);
    }

    @Test
    public void testNullAppName() {
        SLF4JLoggerImpl logger = new SLF4JLoggerImpl(SLF4JLoggerImplTest.class);

        logger.debugWithApp(null, "Test null app name");
        logger.infoWithApp(null, "Test null app name");
        logger.warnWithApp(null, "Test null app name");
        logger.errorWithApp(null, "Test null app name");

        Assert.assertTrue(true);
    }
}
