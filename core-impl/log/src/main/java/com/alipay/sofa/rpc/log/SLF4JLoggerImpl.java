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

import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 * Logger base on native slf4j
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SLF4JLoggerImpl implements Logger {
    private final org.slf4j.Logger logger;

    public SLF4JLoggerImpl(String name) {
        logger = org.slf4j.LoggerFactory.getLogger(name);
    }

    public SLF4JLoggerImpl(Class clazz) {
        logger = org.slf4j.LoggerFactory.getLogger(clazz);
    }

    protected org.slf4j.Logger getLogger() {
        return logger;
    }

    protected org.slf4j.Logger getLogger(String appName) {
        return logger;
    }

    @Override
    public String getName() {
        return getLogger().getName();
    }

    @Override
    public boolean isDebugEnabled() {
        return getLogger().isDebugEnabled();
    }

    @Override
    public void debug(String message) {
        getLogger().debug(message);
    }

    @Override
    public void debug(String format, Object... args) {
        getLogger().debug(format, args);
    }

    @Override
    public void debug(String message, Throwable t) {
        getLogger().debug(message, t);
    }

    @Override
    public boolean isDebugEnabled(String appName) {
        return getLogger(appName).isDebugEnabled();
    }

    @Override
    public void debugWithApp(String appName, String message) {
        getLogger(appName).debug(prefix(appName) + message);
    }

    @Override
    public void debugWithApp(String appName, String format, Object... args) {
        getLogger(appName).debug(prefix(appName) + format, args);
    }

    @Override
    public void debugWithApp(String appName, String message, Throwable t) {
        getLogger(appName).debug(prefix(appName) + message, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return getLogger().isInfoEnabled();
    }

    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void info(String format, Object... args) {
        getLogger().info(format, args);
    }

    @Override
    public void info(String message, Throwable t) {
        getLogger().info(message, t);
    }

    @Override
    public boolean isInfoEnabled(String appName) {
        return getLogger(appName).isInfoEnabled();
    }

    @Override
    public void infoWithApp(String appName, String message) {
        getLogger(appName).info(prefix(appName) + message);
    }

    @Override
    public void infoWithApp(String appName, String format, Object... args) {
        getLogger(appName).info(prefix(appName) + format, args);
    }

    @Override
    public void infoWithApp(String appName, String message, Throwable t) {
        getLogger(appName).info(prefix(appName) + message, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return getLogger().isWarnEnabled();
    }

    @Override
    public void warn(String message) {
        getLogger().warn(message);
    }

    @Override
    public void warn(String format, Object... args) {
        getLogger().warn(format, args);
    }

    @Override
    public void warn(String message, Throwable t) {
        getLogger().warn(message, t);
    }

    @Override
    public boolean isWarnEnabled(String appName) {
        return getLogger(appName).isWarnEnabled();
    }

    @Override
    public void warnWithApp(String appName, String message) {
        getLogger(appName).warn((prefix(appName)) + message);
    }

    @Override
    public void warnWithApp(String appName, String format, Object... args) {
        getLogger(appName).warn(prefix(appName) + format, args);
    }

    @Override
    public void warnWithApp(String appName, String message, Throwable t) {
        getLogger(appName).warn(prefix(appName) + message, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return getLogger().isErrorEnabled();
    }

    @Override
    public void error(String message) {
        getLogger().error(message);
    }

    @Override
    public void error(String format, Object... args) {
        getLogger().error(format, args);
    }

    @Override
    public void error(String message, Throwable t) {
        getLogger().error(message, t);
    }

    @Override
    public boolean isErrorEnabled(String appName) {
        return getLogger(appName).isErrorEnabled();
    }

    @Override
    public void errorWithApp(String appName, String message) {
        getLogger(appName).error(prefix(appName) + message);
    }

    @Override
    public void errorWithApp(String appName, String format, Object... args) {
        getLogger(appName).error(prefix(appName) + format, args);
    }

    @Override
    public void errorWithApp(String appName, String message, Throwable t) {
        getLogger(appName).error(prefix(appName) + message, t);
    }

    private String prefix(String appName) {
        return appName == null ? StringUtils.EMPTY : "[" + appName + "]";
    }
}