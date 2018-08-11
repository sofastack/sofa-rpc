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
package com.alipay.sofa.rpc.protocol;

import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.transport.AbstractChannel;

/**
 * Handler of telnet command
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
@Unstable
public interface TelnetHandler {

    /**
     * The constant LINE.
     */
    String LINE = "\r\n";

    /**
     * Gets command.
     *
     * @return the command
     */
    String getCommand();

    /**
     * Gets description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Do telnet and return string result.
     *
     * @param channel the channel
     * @param message the message
     * @return the string
     */
    String telnet(AbstractChannel channel, String message);
}
