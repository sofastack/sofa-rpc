package com.alipay.sofa.rpc.server.rest;

import org.junit.Test;

public class RemoteIpTest {

    @Test
    public void firstIp() {
        String remoteIP = "1.2.3.4, 6.7.8.9";
        String expect = "1.2.3.4";
        String actual = "";
        int index = remoteIP.indexOf(",");
        if (index > 0) {
            actual = remoteIP.substring(0, index);
        }

        if (!actual.equals(expect)) {
            throw new IllegalStateException("expect first ip " + expect + ", but got " + actual);
        }
    }

}