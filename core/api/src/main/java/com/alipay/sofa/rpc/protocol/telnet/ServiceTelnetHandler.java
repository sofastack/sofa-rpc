package com.alipay.sofa.rpc.protocol.telnet;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.protocol.ConsumerConfigRepository;
import com.alipay.sofa.rpc.protocol.ProviderConfigRepository;
import com.alipay.sofa.rpc.protocol.TelnetHandler;

@Extension("service")
public class ServiceTelnetHandler implements TelnetHandler {
    @Override
    public String getCommand() {
        return "service";
    }

    @Override
    public String getDescription() {
        return "\t : "+"[<app>] show providerconfig!";
    }

    @Override
    public String telnet(String message) {


        StringBuilder result = new StringBuilder(80000000);
        ProviderConfigRepository providerConfigRepository = ProviderConfigRepository.getProviderConfigRepository();
        ConsumerConfigRepository consumerConfigRepository = ConsumerConfigRepository.getConsumerConfigRepository();

        String[] syntax = message.split("\\s+");
        if (syntax.length != 1) {
            if (!providerConfigRepository.getProvidedServiceMap().containsKey(syntax[1])) {
                result.append("The Service is not provided");
                return result.toString();
            }

        } else {
            result.append("The InterfaceId cannot be null,please type help");
            return result.toString();
        }


        ProviderConfig providerConfig = providerConfigRepository.getProviderConfig(syntax[1]);
        result.append("ProviderConfig:\r\n");
        result.append("Ref:\t" + TAP + providerConfig.getRef() + "\r\n");
        result.append("Server:\t" + TAP + providerConfig.getServer() + "\r\n");
        result.append("Delay:\t" + TAP + providerConfig.getDelay() + "\r\n");
        result.append("Weight:\t" + TAP + providerConfig.getWeight() + "\r\n");
        result.append("Include:" + TAP + providerConfig.getInclude() + "\r\n");
        result.append("Exclude:" + TAP + providerConfig.getExclude() + "\r\n");
        result.append("Dynamic:" + TAP + providerConfig.isDynamic() + "\r\n");
        result.append("Priority:" + TAP + providerConfig.getPriority() + "\r\n");
        result.append("Bootstrap:" + TAP + providerConfig.getBootstrap() + "\r\n");
        result.append("Executor:" + TAP + providerConfig.getExecutor() + "\r\n");
        result.append("Timeout:" + TAP + providerConfig.getTimeout() + "\r\n");
        result.append("Concurrents:" + TAP + providerConfig.getConcurrents() + "\r\n");
        result.append("RepeatedExportLimit:\t\t" + providerConfig.getRepeatedExportLimit() + "\r\n");
        result.append("MethodsLimit:" + TAP + providerConfig.getMethodsLimit() + "\r\n");
        result.append("ProviderBootstrap:\t\t" + providerConfig.getProviderBootstrap() + "\r\n");

        return result.toString();
    }
}
