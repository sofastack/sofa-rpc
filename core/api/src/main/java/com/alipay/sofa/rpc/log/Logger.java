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

/**
 * Just Logger.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public interface Logger {

    /**
     * Gets name.
     *
     * @return the name
     */
    String getName();

    /**
     * Is debug enabled boolean.
     *
     * @return the boolean
     */
    boolean isDebugEnabled();

    /**
     * Debug.
     *
     * @param message the message
     */
    void debug(String message);

    /**
     * Debug.
     *
     * @param format the format
     * @param args   the args
     */
    void debug(String format, Object... args);

    /**
     * Debug.
     *
     * @param message the message
     * @param t       the t
     */
    void debug(String message, Throwable t);

    /**
     * Is debug enabled boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    boolean isDebugEnabled(String appName);

    /**
     * Debug with app.
     *
     * @param appName the app name
     * @param message the message
     */
    void debugWithApp(String appName, String message);

    /**
     * Debug with app.
     *
     * @param appName the app name
     * @param format  the format
     * @param args    the args
     */
    void debugWithApp(String appName, String format, Object... args);

    /**
     * Debug with app.
     *
     * @param appName the app name
     * @param message the message
     * @param t       the t
     */
    void debugWithApp(String appName, String message, Throwable t);

    /**
     * Is info enabled boolean.
     *
     * @return the boolean
     */
    boolean isInfoEnabled();

    /**
     * Info.
     *
     * @param message the message
     */
    void info(String message);

    /**
     * Info.
     *
     * @param format the format
     * @param args   the args
     */
    void info(String format, Object... args);

    /**
     * Info.
     *
     * @param message the message
     * @param t       the t
     */
    void info(String message, Throwable t);

    /**
     * Is info enabled boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    boolean isInfoEnabled(String appName);

    /**
     * Info with app.
     *
     * @param appName the app name
     * @param message the message
     */
    void infoWithApp(String appName, String message);

    /**
     * Info with app.
     *
     * @param appName the app name
     * @param format  the format
     * @param args    the args
     */
    void infoWithApp(String appName, String format, Object... args);

    /**
     * Info with app.
     *
     * @param appName the app name
     * @param message the message
     * @param t       the t
     */
    void infoWithApp(String appName, String message, Throwable t);

    /**
     * Is warn enabled boolean.
     *
     * @return the boolean
     */
    boolean isWarnEnabled();

    /**
     * Warn.
     *
     * @param message the message
     */
    void warn(String message);

    /**
     * Warn.
     *
     * @param format the format
     * @param args   the args
     */
    void warn(String format, Object... args);

    /**
     * Warn.
     *
     * @param message the message
     * @param t       the t
     */
    void warn(String message, Throwable t);

    /**
     * Is warn enabled boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    boolean isWarnEnabled(String appName);

    /**
     * Warn with app.
     *
     * @param appName the app name
     * @param message the message
     */
    void warnWithApp(String appName, String message);

    /**
     * Warn with app.
     *
     * @param appName the app name
     * @param format  the format
     * @param args    the args
     */
    void warnWithApp(String appName, String format, Object... args);

    /**
     * Warn with app.
     *
     * @param appName the app name
     * @param message the message
     * @param t       the t
     */
    void warnWithApp(String appName, String message, Throwable t);

    /**
     * Is error enabled boolean.
     *
     * @return the boolean
     */
    boolean isErrorEnabled();

    /**
     * Error.
     *
     * @param message the message
     */
    void error(String message);

    /**
     * Error.
     *
     * @param format the format
     * @param args   the args
     */
    void error(String format, Object... args);

    /**
     * Error.
     *
     * @param message the message
     * @param t       the t
     */
    void error(String message, Throwable t);

    /**
     * Is error enabled boolean.
     *
     * @param appName the app name
     * @return the boolean
     */
    boolean isErrorEnabled(String appName);

    /**
     * Error with app.
     *
     * @param appName the app name
     * @param message the message
     */
    void errorWithApp(String appName, String message);

    /**
     * Error with app.
     *
     * @param appName the app name
     * @param format  the format
     * @param args    the args
     */
    void errorWithApp(String appName, String format, Object... args);

    /**
     * Error with app.
     *
     * @param appName the app name
     * @param message the message
     * @param t       the t
     */
    void errorWithApp(String appName, String message, Throwable t);

}
