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
package com.alipay.sofa.rpc.transport;

/**
 * StreamHandler, works just like gRPC StreamObserver.
 */
public interface SofaStreamObserver<T> {

    /**
     * Sends a message, or defines the behavior when a message is received.
     * <p>This method should never be called after {@link SofaStreamObserver#onCompleted()} has been invoked.
     */
    void onNext(T message);

    /**
     * Note: This method MUST be invoked after the transport is complete.
     * Failure to do so may result in unexpected errors.
     * <p>
     * Signals that all messages have been sent/received normally, and closes this stream.
     */
    void onCompleted();

    /**
     * Signals an exception to terminate this stream, or defines the behavior when an error occurs.
     * <p></p>
     * Once this method is invoked by one side, it can't send more messages, and the corresponding method on the other side will be triggered.
     * Depending on the protocol implementation, it's possible that the other side can still call {@link SofaStreamObserver#onNext(Object)} after this method has been invoked, although this is not recommended.
     * <p></p>
     * As a best practice, it is advised not to send any more information once this method is called.
     *
     */
    void onError(Throwable throwable);
}
