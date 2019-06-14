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

import static com.google.common.base.Preconditions.checkArgument;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.etcd.EtcdRegistry;
import com.alipay.sofa.rpc.registry.etcd.Watcher;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.DeleteRangeRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.Event;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.KVGrpc;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.KeyValue;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseGrantRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseGrantResponse;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseGrpc;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseKeepAliveRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseKeepAliveResponse;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseRevokeRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.LeaseRevokeResponse;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.PutRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.RangeRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.WatchCancelRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.WatchCreateRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.WatchGrpc;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.WatchRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.WatchResponse;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.StringUtil;
import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @created 2018/6/5
 **/
public class EtcdClient implements Closeable {

    private final static Logger                    LOGGER               = LoggerFactory
                                                                            .getLogger(EtcdRegistry.class);

    protected static final long                    DEFAULT_TTL          = 10;                              // default ttl time for the key in seconds
    private volatile long                          leaseId;
    private ManagedChannel                         channel;
    private final KVGrpc.KVBlockingStub            kvBlockingStub;
    private final LeaseGrpc.LeaseBlockingStub      leaseBlockingStub;
    private final LeaseGrpc.LeaseStub              leaseStub;
    private final WatchGrpc.WatchStub              watchStub;
    private volatile boolean                       keepAliveTaskStarted = false;
    private StreamObserver<LeaseKeepAliveResponse> leaseKeepAliveResponseObserver;
    private StreamObserver<LeaseKeepAliveRequest>  leaseKeepAliveRequestObserver;
    private final ScheduledExecutorService         scheduledExecutorService;
    private ScheduledFuture<?>                     keepAliveTaskFuture;
    private volatile AtomicInteger retryKeepalive = new AtomicInteger(3);

    EtcdClient(ManagedChannel channel) {
        this.channel = channel;
        this.kvBlockingStub = KVGrpc.newBlockingStub(this.channel);
        this.leaseBlockingStub = LeaseGrpc.newBlockingStub(this.channel);
        this.leaseStub = LeaseGrpc.newStub(this.channel);
        this.watchStub = WatchGrpc.newStub(this.channel);
        this.scheduledExecutorService = MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("EtcdRegisterKeepAliveTaskThread", true)));
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    /**
     * reuse a lease and bind to the key
     */
    public void putWithLease(final String key, final String value) {
        if (leaseId == 0L) {
            LeaseGrantRequest leaseGrantRequest = LeaseGrantRequest.newBuilder().setTTL(DEFAULT_TTL).build();
            LeaseGrantResponse leaseGrantResponse = leaseBlockingStub.leaseGrant(leaseGrantRequest);
            leaseId = leaseGrantResponse.getID();
        }
        PutRequest putRequest = PutRequest.newBuilder().setKey(ByteString.copyFromUtf8(key))
            .setLease(leaseId)
            .setValue(ByteString.copyFromUtf8(value)).build();
        kvBlockingStub.put(putRequest);
    }

    /**
     * get value by key with <b>--prefix</b> flag
     *
     * @return key value list
     */
    public List<KeyValue> getWithPrefix(String key) {
        checkArgument(!StringUtil.isNullOrEmpty(key), "key cannot be null or empty");
        ByteString keyByte = ByteString.copyFromUtf8(key);
        RangeRequest request = RangeRequest.newBuilder().setKey(keyByte)
            .setRangeEnd(EtcdClientUtil.plusOne(keyByte)).build();
        return kvBlockingStub.range(request).getKvsList();
    }

    public boolean revokeLease(final Long leaseId) {
        try {
            LeaseRevokeRequest revokeRequest = LeaseRevokeRequest.newBuilder().setID(leaseId).build();
            LeaseRevokeResponse revokeResponse = leaseBlockingStub.leaseRevoke(revokeRequest);
            return revokeResponse != null;
        } catch (Exception e) {
            LOGGER.error("cannot revoke lease id:{}", leaseId, e);
        }
        return false;
    }

    public void keepAlive() {
        if (leaseId == 0L) {
            return;
        }

        if (!this.keepAliveTaskStarted) {
            this.keepAliveTaskStarted = true;
            this.startKeepAliveTask();
        }
    }

    private void startKeepAliveTask() {
        //no lease id found, return
        if (leaseId == 0L) {
            this.keepAliveTaskStarted = false;
            return;
        }

        this.leaseKeepAliveResponseObserver =
                new StreamObserver<LeaseKeepAliveResponse>() {
                    @Override
                    public void onNext(LeaseKeepAliveResponse value) {
                        processKeepAliveResponse(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.error("keep alive for lease id:{} error", leaseId, t);
                        processOnError();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.debug("lease keep alive task is completed");
                    }
                };
        this.leaseKeepAliveRequestObserver = this.leaseStub.leaseKeepAlive(this.leaseKeepAliveResponseObserver);
        this.keepAliveTaskFuture = scheduledExecutorService.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    leaseKeepAliveRequestObserver.onNext(LeaseKeepAliveRequest.newBuilder().setID(leaseId).build());
                }
            },
            0, DEFAULT_TTL / 3, TimeUnit.SECONDS
            );
    }

    /**
     * restart keep alive task
     */
    private void processOnError() {
        this.keepAliveTaskFuture.cancel(true);
        this.leaseKeepAliveRequestObserver.onCompleted();
        this.leaseKeepAliveResponseObserver.onCompleted();
        if(retryKeepalive.getAndDecrement() <=0){
            this.startKeepAliveTask();
        }
    }

    private void processKeepAliveResponse(LeaseKeepAliveResponse value) {
        final long ttl = value.getTTL();
        if (ttl <= 0) {
            //reset global lease id
            leaseId = 0L;
        }
    }

    /**
     * delete one key
     */
    public void deleteKey(String key) {
        kvBlockingStub.deleteRange(DeleteRangeRequest.newBuilder().setKey(ByteString.copyFromUtf8(key)).build());
    }

    public void startWatch(String key, final Watcher watcher) {
        StreamObserver<WatchRequest> request = watchStub.watch(new StreamObserver<WatchResponse>() {
            @Override
            public void onNext(WatchResponse watchResponse) {
                if (watchResponse.getCreated()) {
                    watcher.setWatchId(watchResponse.getWatchId());
                }

                if (watchResponse.getCanceled()) {
                    LOGGER.debug("watch:{} is canceled", watchResponse.getWatchId());
                }
                if (watchResponse.getEventsList().size() > 0) {
                    try {
                        watcher.call();
                    } catch (Exception e) {
                        StringBuilder stringBuilder = new StringBuilder(
                            "call watcher update method error, watch id:{}, events:");
                        for (Event event : watchResponse.getEventsList()) {
                            stringBuilder.append(event.getType().name()).append(" ");
                        }
                        LOGGER.debug(stringBuilder.toString(), watchResponse.getWatchId(), e);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.debug("start watch error for watcher: {}", watcher.toString());
            }

            @Override
            public void onCompleted() {
                //do nothing
            }
        });
        ByteString keyByte = ByteString.copyFromUtf8(key);
        WatchCreateRequest createRequest = WatchCreateRequest.newBuilder().setKey(keyByte)
            .setRangeEnd(EtcdClientUtil.plusOne(keyByte)).build();
        request.onNext(WatchRequest.newBuilder().setCreateRequest(createRequest).build());
    }

    public void cancelWatch(final Watcher watcher) {
        if (watcher.getWatchId() == null) {
            LOGGER.info("watch id is blank when cancel watch, return with doing nothing");
            return;
        }
        StreamObserver<WatchRequest> watchRequest = watchStub.watch(new StreamObserver<WatchResponse>() {
            @Override
            public void onNext(WatchResponse watchResponse) {
                if (watchResponse.getCanceled()) {
                    watcher.setWatchId(null);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.debug("error happened when cancel watch for watcher:{}", watcher, throwable);
            }

            @Override
            public void onCompleted() {
                //do nothing
            }
        });
        watchRequest.onNext(WatchRequest.newBuilder()
            .setCancelRequest(WatchCancelRequest.newBuilder().setWatchId(watcher.getWatchId()).build()).build());
    }

    @Override
    public void close() {
        if (channel != null) {
            this.channel.shutdown();
        }

        try {
            leaseKeepAliveRequestObserver.onCompleted();
        } catch (Exception e) {
            //do nothing
        }

        try{
            leaseKeepAliveResponseObserver.onCompleted();
        }catch (Exception e){
            //do nothing
        }

        if (leaseId != 0L) {
            try {
                revokeLease(leaseId);
            } catch (Exception e) {
                LOGGER.error("lease id:{} may not be revoked", e);
            }
        }

        keepAliveTaskFuture.cancel(true);
    }
}
