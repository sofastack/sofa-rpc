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
package com.alipay.sofa.rpc.common.json;

/**
 * Exception when parse json
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ParseException extends RuntimeException {
    private static final long serialVersionUID = 2560442967697088006L;
    private int               position         = 0;
    private String            jsonString       = "";

    /**
     * Constructs a new json exception with the specified detail message.
     *
     * @param json     the json text which cause JSONParseException
     * @param position the position of illegal escape char at json text;
     * @param message  the detail message. The detail message is saved for
     *                 later retrieval by the {@link #getMessage()} method.
     */
    public ParseException(String json, int position, String message) {
        super(message);
        this.jsonString = json;
        this.position = position;
    }

    /**
     * Get message about error when parsing illegal json
     *
     * @return error message
     */
    @Override
    public String getMessage() {
        final int maxTipLength = 10;
        int end = position + 1;
        int start = end - maxTipLength;
        if (start < 0) {
            start = 0;
        }
        if (end > jsonString.length()) {
            end = jsonString.length();
        }
        return String.format("%s (%d):%s", jsonString.substring(start, end), position, super.getMessage());
    }

    public String getJson() {
        return this.jsonString;
    }

    public int getPosition() {
        return this.position;
    }

}
