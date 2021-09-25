package com.alipay.sofa.rpc.protocol.telnet;


import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.protocol.TelnetHandler;
import com.alipay.sofa.rpc.protocol.TelnetHandlerFactory;

/**
 *
 */
public class TelnetCommandHandler {
    public final static String TELNET_STRING_END = new String(new byte[]{(byte) 13, (byte) 10});


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
