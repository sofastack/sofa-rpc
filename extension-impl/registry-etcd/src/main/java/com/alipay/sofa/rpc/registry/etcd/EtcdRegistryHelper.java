package com.alipay.sofa.rpc.registry.etcd;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;

import java.util.UUID;

public class EtcdRegistryHelper extends RegistryUtils {


    /**
     * make unique key by appending uuid to config path
     *
     * @param rootPath
     * @param config
     * @return
     */
    public static String buildUniqueKey(String rootPath, ProviderConfig config) {
        return buildConfigPath(rootPath, config) + ":" + UUID.randomUUID().toString();
    }
}
