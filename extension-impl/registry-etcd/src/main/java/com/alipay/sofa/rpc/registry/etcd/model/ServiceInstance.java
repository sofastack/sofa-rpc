package com.alipay.sofa.rpc.registry.etcd.model;

import com.alipay.sofa.rpc.config.ProviderConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * keep service information
 */
public class ServiceInstance {
    private String interfaceId;
    private String host;
    private int port;
    private String clusterName;
    private String serviceName;
    private Map<String, String> metadata = new HashMap<String,String>();

}
