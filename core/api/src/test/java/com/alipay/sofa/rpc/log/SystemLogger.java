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

import com.alipay.sofa.rpc.common.annotation.JustForTest;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@JustForTest
public class SystemLogger implements Logger {

    String name;

    public SystemLogger(String name) {
        this.name = name;
    }

    public SystemLogger(Class clazz) {
        this.name = clazz.getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        System.out.println("[DEBUG]" + msg);
    }

    @Override
    public void debug(String format, Object... args) {
        System.out.println("[DEBUG]" + toString(format, args));
    }

    @Override
    public void debug(String message, Throwable t) {
        System.err.println("[DEBUG]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isDebugEnabled(String appName) {
        return true;
    }

    @Override
    public void debugWithApp(String appName, String msg) {
        System.out.println("[DEBUG]" + msg);
    }

    @Override
    public void debugWithApp(String appName, String format, Object... args) {
        System.out.println("[DEBUG]" + toString(format, args));
    }

    @Override
    public void debugWithApp(String appName, String message, Throwable t) {
        System.err.println("[DEBUG]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        System.out.println("[INFO]" + msg);
    }

    @Override
    public void info(String format, Object... args) {
        System.out.println("[INFO]" + toString(format, args));
    }

    @Override
    public void info(String message, Throwable t) {
        System.err.println("[INFO]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isInfoEnabled(String appName) {
        return true;
    }

    @Override
    public void infoWithApp(String appName, String msg) {
        System.out.println("[INFO]" + msg);
    }

    @Override
    public void infoWithApp(String appName, String format, Object... args) {
        System.out.println("[INFO]" + toString(format, args));
    }

    @Override
    public void infoWithApp(String appName, String message, Throwable t) {
        System.err.println("[INFO]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        System.out.println("[WARN]" + msg);
    }

    @Override
    public void warn(String format, Object... args) {
        System.out.println("[WARN]" + toString(format, args));
    }

    @Override
    public void warn(String message, Throwable t) {
        System.err.println("[WARN]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isWarnEnabled(String appName) {
        return true;
    }

    @Override
    public void warnWithApp(String appName, String msg) {
        System.out.println("[WARN]" + msg);
    }

    @Override
    public void warnWithApp(String appName, String format, Object... args) {
        System.out.println("[WARN]" + toString(format, args));
    }

    @Override
    public void warnWithApp(String appName, String message, Throwable t) {
        System.err.println("[WARN]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        System.out.println("[ERROR]" + msg);
    }

    @Override
    public void error(String format, Object... args) {
        System.out.println("[ERROR]" + toString(format, args));
    }

    @Override
    public void error(String message, Throwable t) {
        System.err.println("[ERROR]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    @Override
    public boolean isErrorEnabled(String appName) {
        return true;
    }

    @Override
    public void errorWithApp(String appName, String msg) {
        System.out.println("[ERROR]" + msg);
    }

    @Override
    public void errorWithApp(String appName, String format, Object... args) {
        System.out.println("[ERROR]" + toString(format, args));
    }

    @Override
    public void errorWithApp(String appName, String message, Throwable t) {
        System.err.println("[ERROR]" + "[" + getName() + "] " + message);
        t.printStackTrace();
    }

    private String toString(String format, Object[] args) {
        if (CommonUtils.isEmpty(args)) {
            return format;
        }
        String s = format.replaceAll("\\{\\}", "%s");
        String[] ss = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            ss[i] = StringUtils.toString(args[i]);
        }
        return String.format(s, ss);
    }
}
