/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package com.alipay.sofa.rpc.server.triple;

import io.grpc.stub.StreamObserver;

/**
 * @author zhaowang
 * @version : MockStreamObserver.java, v 0.1 2020年06月30日 11:58 上午 zhaowang Exp $
 */
public class MockStreamObserver<V> implements StreamObserver<V> {

    private V value;

    public V getValue() {
        return value;
    }

    @Override
    public void onNext(V value) {
        this.value = value;
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }
}