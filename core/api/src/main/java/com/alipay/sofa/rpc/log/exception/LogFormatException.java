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
package com.alipay.sofa.rpc.log.exception;

/**
 * 当日志格式化的时候抛出的异常
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>hongwei.yhw</a>
 */
public class LogFormatException extends RuntimeException {

    public LogFormatException(String code) {
        super("LogCode: " + code);
    }

    public LogFormatException(String code, Throwable t) {
        super("LogCode: " + code, t);
    }

}
