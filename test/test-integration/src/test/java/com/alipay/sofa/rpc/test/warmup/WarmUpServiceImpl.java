/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.alipay.sofa.rpc.test.warmup;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liengen</a>
 * @version $Id: WamUpServiceImpl.java, v 0.1 2018年04月23日 上午11:06 LiWei.Liengen Exp $
 */
public class WarmUpServiceImpl implements WarmUpService{

    private final int port;

    public WarmUpServiceImpl(int port) {
        this.port = port;
    }

    @Override
    public int getPort() {
        return port;
    }
}