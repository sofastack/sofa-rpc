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

import com.alipay.sofa.rpc.registry.etcd.grpc.api.AuthGrpc;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.AuthenticateRequest;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.AuthenticateResponse;
import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.NameResolver.Factory;
import io.grpc.Status.Code;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A builder class for creating etcd client
 *
 * @author Fuwenming
 * @created 2018/6/7
 **/
public class ClientBuilder {

    private static final Metadata.Key<String> TOKEN_KEY         = Metadata.Key.of("token",
                                                                    Metadata.ASCII_STRING_MARSHALLER);
    private static final int                  DEFAULT_ETCD_PORT = 2379;
    //one client should only has one token
    private final AtomicReference<String>     token             = new AtomicReference<String>();
    private static final String               ETCD              = "etcd";
    private NettyChannelBuilder               channelBuilder;

    // not strict hostname validation
    private static final Pattern              ENDPOINT_PATTERN  =
                                                                        Pattern
                                                                            .compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):?(\\d{1,5})?");

    private AuthGrpc.AuthBlockingStub         authBlockingStub;
    private ByteString                        name;
    private ByteString                        password;
    private final AtomicBoolean               authEnabled       = new AtomicBoolean(true);

    public ClientBuilder auth(String name, String password) {
        if (StringUtil.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("try to authenticate while name is blank");
        }
        this.name = ByteString.copyFromUtf8(name);
        this.password = ByteString.copyFromUtf8(password);
        return this;
    }

    public ClientBuilder endpoints(String endpoints) {
        if (StringUtil.isNullOrEmpty(endpoints)) {
            throw new IllegalArgumentException("endpoints should not be null or empty");
        }
        final String[] endpointArray = endpoints.split(",");
        channelBuilder = NettyChannelBuilder.forTarget(ETCD)
            .nameResolverFactory(new SimpleNameResolver(endpointArray));
        return this;
    }

    public EtcdClient build() {
        if (this.name == null || this.password == null) {
            authEnabled.set(false);
        }
        ManagedChannel channel = channelBuilder.usePlaintext().intercept(new AddTokenInterceptor())
            .idleTimeout(30, TimeUnit.SECONDS)
            .build();
        return new EtcdClient(channel);
    }

    /**
     * add token for each client request, and refresh token when token is expired
     * <br />
     * Nothing will be done with the request except for adding a token
     */
    private class AddTokenInterceptor implements ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                                                                   MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, final Channel next) {
            return new SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    if (authEnabled.get()) {
                        headers.put(TOKEN_KEY, getToken(next));
                    }
                    super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
                        @Override
                        public void onClose(Status status, Metadata trailers) {
                            updateToken(status, next);
                            super.onClose(status, trailers);
                        }
                    }, headers);
                }
            };
        }
    }

    /**
     * will try to auth and get token when sending request to etcd at first time
     *
     * @param channel the client channel
     * @return token string get from etcd
     */
    private String getToken(Channel channel) {
        String tokenString = token.get();
        if (StringUtil.isNullOrEmpty(tokenString)) {
            synchronized (token) {
                tokenString = token.get();
                if (StringUtil.isNullOrEmpty(tokenString)) {
                    //get token from authentication
                    tokenString = auth(channel);
                    token.lazySet(tokenString);
                }
            }
        }
        return tokenString;
    }

    /**
     * try to do the authentication with username and password provided
     *
     * @param channel the client channel
     * @return token from etcd server
     */
    private String auth(Channel channel) {
        if (!authEnabled.get()) {
            return "";
        }
        if (authBlockingStub == null) {
            authBlockingStub = AuthGrpc.newBlockingStub(channel);
        }
        AuthenticateRequest request = AuthenticateRequest.newBuilder().setNameBytes(this.name)
            .setPasswordBytes(this.password).build();
        AuthenticateResponse response = authBlockingStub.authenticate(request);
        return response.getToken();
    }

    /**
     * update token when token is invalid
     *
     * @param status the status of the etcd operation
     * @param channel the client channel
     */
    private void updateToken(Status status, Channel channel) {
        if (isTokenInvalid(status)) {
            try {
                synchronized (token) {
                    token.lazySet(auth(channel));
                }
            } catch (Exception e) {
                //log it
            }
        }
    }

    /**
     * check token status
     */
    private boolean isTokenInvalid(Status status) {
        return status.getCode() == Code.UNAUTHENTICATED
            && !StringUtil.isNullOrEmpty(status.getDescription())
            && status.getDescription().contains("invalid auth token");
    }

    /**
     * a simple name resolver for server endpoints
     * <br />
     * endpoint strings as following are supported
     * <br />
     * <li>192.168.1.1:2222,192.168.1.2:2322,...</li>
     * <li>192.168.1.1,192.168.1.2:2322,192.168.1.3,...</li>
     * <li>192.168.1.1:2222</li>
     * <li>192.168.1.1</li>
     */
    private class SimpleNameResolver extends Factory {

        private final String[] endpointArray;

        SimpleNameResolver(String... endpointArray) {
            this.endpointArray = endpointArray;
        }

        @Override
        public NameResolver newNameResolver(URI targetUri, Attributes params) {
            if (!ETCD.equals(targetUri.getScheme())) {
                return null;
            }
            return new NameResolver() {
                @Override
                public void start(Listener listener) {
                    List<SocketAddress> socketAddressList = new ArrayList<SocketAddress>(
                        endpointArray.length);
                    try {
                        for (String endpoint : endpointArray) {
                            Matcher m = ENDPOINT_PATTERN.matcher(endpoint.trim());
                            if (!m.matches()) {
                                throw new Exception("invalid endpoint: " + endpoint);
                            }
                            String hostname = m.group(1);
                            String portStr = m.group(2);
                            int port = portStr != null ? Integer.parseInt(portStr) : DEFAULT_ETCD_PORT;
                            socketAddressList.add(new InetSocketAddress(hostname, port));
                        }
                        Collections.shuffle(socketAddressList);
                        List<EquivalentAddressGroup> addressGroup = Collections
                            .singletonList(new EquivalentAddressGroup(socketAddressList));
                        listener.onAddresses(addressGroup, Attributes.EMPTY);
                    } catch (Exception e) {
                        listener.onError(Status.INVALID_ARGUMENT.withCause(e).withDescription(e.getMessage()));
                    }
                }

                @Override
                public String getServiceAuthority() {
                    return ETCD;
                }

                @Override
                public void shutdown() {
                    //log it
                }
            };
        }

        @Override
        public String getDefaultScheme() {
            return ETCD;
        }
    }
}
