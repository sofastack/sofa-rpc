/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.alipay.sofa.rpc.registry.zk;

/**
 *
 * @author zhuoyu.sjw
 * @version $Id: OverrideServiceImpl.java, v 0.1 2018-06-16 09:51 zhuoyu.sjw Exp $$
 */
public class OverrideServiceImpl implements OverrideService {

    private int port;

    public OverrideServiceImpl(int port) {
        this.port = port;
    }

    @Override
    public int getPort() {
        return port;
    }
}
