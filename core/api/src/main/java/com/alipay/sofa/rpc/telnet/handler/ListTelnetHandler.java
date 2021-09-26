package com.alipay.sofa.rpc.telnet.handler;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.telnet.cache.ConsumerConfigRepository;
import com.alipay.sofa.rpc.telnet.cache.ProviderConfigRepository;
import com.alipay.sofa.rpc.telnet.TelnetHandler;

import java.util.ArrayList;

@Extension("list")
public class ListTelnetHandler implements TelnetHandler {
    @Override
    public String getCommand() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "\t : " + "list [-p] show Provided Services list; list [-c] show Referred Services list";
    }

    @Override
    public String telnet(String message) {
        StringBuilder buf = new StringBuilder();
        String service = null;
        boolean printAllService = false;
        boolean printProviderService = false;
        boolean printComsumerService = false;

        String[] parts = message.split("\\s+");
        for (String part : parts) {
            if ("-p".equals(part)) {
                printProviderService = true;
            } else if ("-c".equals(part)) {
                printComsumerService = true;
            } else {
                if (!StringUtils.isEmpty(service)) {
                    return "Invalid parameter " + part;
                }
                service = part;
            }
        }

        if (parts.length == 1) {
            printAllServices(buf, true, true, true);

        } else {
            printAllServices(buf, printAllService, printProviderService, printComsumerService);
        }


        return buf.toString();
    }

    private void printAllServices(StringBuilder buf, boolean printAllService, boolean printProviderService, boolean printComsumerService) {
        printAllProvidedServices(buf, printProviderService);
        printAllReferredServices(buf, printComsumerService);
    }

    private void printAllProvidedServices(StringBuilder buf, boolean printProviderService) {
        if (printProviderService) {
            ProviderConfigRepository providerConfigRepository = ProviderConfigRepository.getProviderConfigRepository();
            ArrayList<ProviderConfig> providedServiceList = providerConfigRepository.getProvidedServiceList();
            if (!providedServiceList.isEmpty()) {
                buf.append("PROVIDER:\r\n");
            }

            for (ProviderConfig providerConfig : providedServiceList) {
                buf.append(providerConfig.getInterfaceId());
                buf.append("\r\n");
            }
        }

    }

    private void printAllReferredServices(StringBuilder buf, boolean printComsumerService) {
        if (printComsumerService) {
            ConsumerConfigRepository consumerConfigRepository = ConsumerConfigRepository.getConsumerConfigRepository();
            ArrayList<ConsumerConfig> consumerConfigList = consumerConfigRepository.getReferredServiceList();
            if (!consumerConfigList.isEmpty()) {
                buf.append("CONSUMER:\r\n");
            }

            for (ConsumerConfig consumerConfig : consumerConfigList) {
                buf.append(consumerConfig.getInterfaceId());
                buf.append("\r\n");
            }
        }

    }
}
