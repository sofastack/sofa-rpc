package com.alipay.sofa.rpc.codec.fury;

public enum AccessConfig {
                          WHITELIST_CONFIG("whitelist"), BLACKLIST_CONFIG("blacklist"),

                          NONE_CONFIG("none");

    private final String configType;

    AccessConfig(String configType) {
        this.configType = configType;
    }

    public String getConfigType() {
        return configType;
    }
}
