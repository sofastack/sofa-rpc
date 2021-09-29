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
package com.alipay.sofa.rpc.telnet;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.telnet.handler.HelpTelnetHandler;

/**
 *
 */
public class TelnetCommandHandler {
    public final static String TELNET_STRING_END = new String(new byte[] { (byte) 13, (byte) 10 });

    public static String handleCommand(String cmdLine) {
        StringBuffer handleResult = new StringBuffer();
        String[] command = cmdLine.split("\\s");
        if (TelnetHandlerFactory.getAllHandlers().containsKey(command[0])) {
            TelnetHandler handler = TelnetHandlerFactory.getHandler(command[0]);
            handleResult.append(handler.telnet(cmdLine));
        } else {
            handleResult.append(helpMessage(command[0]));
        }
        return handleResult.toString();
    }

    public static String helpMessage(String cmdLine) {
        HelpTelnetHandler helpTelnetHandler = new HelpTelnetHandler();
        return helpTelnetHandler.telnet(cmdLine);
    }

    public static String promptMessage() {
        String prompt = "sofa-rpc>";
        return prompt;
    }

    public static String responseMessage(String cmd) {
        String commandResult = handleCommand(cmd);
        commandResult = commandResult.replace("\n", TELNET_STRING_END);
        if (StringUtils.isEmpty(commandResult)) {
            commandResult = TELNET_STRING_END;
        } else if (!commandResult.endsWith(TELNET_STRING_END)) {
            commandResult = commandResult + TELNET_STRING_END
                + TELNET_STRING_END;
        } else if (!commandResult.endsWith(TELNET_STRING_END
            .concat(TELNET_STRING_END))) {
            commandResult = commandResult + TELNET_STRING_END;
        }
        commandResult = commandResult + promptMessage();
        return commandResult;
    }
}
