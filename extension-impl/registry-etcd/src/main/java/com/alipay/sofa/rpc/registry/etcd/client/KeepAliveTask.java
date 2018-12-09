/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.registry.etcd.client;

import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseKeepAliveResponse;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * keep alive task information model
 */
public class KeepAliveTask implements StreamObserver<LeaseKeepAliveResponse> {

    private final List<StreamObserver<LeaseKeepAliveResponse>> responseObservers;

    private long                                               expireTime;
    private long                                               nextKeepAliveTime;

    public KeepAliveTask() {
        this.nextKeepAliveTime = System.currentTimeMillis();
        this.expireTime = nextKeepAliveTime + EtcdClient.DEFAULT_TTL * 1000;
        this.responseObservers = new CopyOnWriteArrayList<StreamObserver<LeaseKeepAliveResponse>>();
    }

    public void addObserver(StreamObserver<LeaseKeepAliveResponse> observer) {
        this.responseObservers.add(observer);
    }

    @Override
    public void onNext(LeaseKeepAliveResponse response) {
        for (StreamObserver<LeaseKeepAliveResponse> observer : responseObservers) {
            observer.onNext(response);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        for (StreamObserver<LeaseKeepAliveResponse> observer : responseObservers) {
            observer.onError(new RuntimeException(throwable));
        }
    }

    @Override
    public void onCompleted() {
        for (StreamObserver<LeaseKeepAliveResponse> observer : responseObservers) {
            observer.onCompleted();
        }
        this.responseObservers.clear();
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getNextKeepAliveTime() {
        return nextKeepAliveTime;
    }

    public void setNextKeepAliveTime(long nextKeepAliveTime) {
        this.nextKeepAliveTime = nextKeepAliveTime;
    }
}
