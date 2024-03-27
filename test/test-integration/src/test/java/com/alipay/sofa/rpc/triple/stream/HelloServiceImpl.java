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
package com.alipay.sofa.rpc.triple.stream;

import com.alipay.sofa.rpc.transport.StreamHandler;

public class HelloServiceImpl implements HelloService {

    @Override
    public void sayHello() {
        System.out.println("Get hello from consumer!");
    }

    @Override
    public void sayHello(String msg) {
        System.out.println("Get " + msg + "from consumer");
    }

    @Override
    public String sayHelloUnary(String message) {
        System.out.println("Get hello from consumer and try response...");
        return "Hello too, " + message;
    }

    @Override
    public StreamHandler<ClientRequest> sayHelloBiStream(StreamHandler<ServerResponse> streamHandler) {
        return new ClientRequestEchoHandler(streamHandler);
    }

    @Override
    public void sayHelloServerStream(StreamHandler<ServerResponse> streamHandler, ClientRequest clientRequest) {
        streamHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount()));
        streamHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 1));
        streamHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 2));
        streamHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 3));
        streamHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 4));
        if (clientRequest.getMsg().equals(HelloService.CMD_TRIGGER_STEAM_ERROR)) {
            streamHandler.onException(new RuntimeException(HelloService.ERROR_MSG));
        } else {
            streamHandler.onFinish();
        }
    }

    static class ClientRequestEchoHandler implements StreamHandler<ClientRequest> {

        StreamHandler<ServerResponse> respHandler;

        public ClientRequestEchoHandler(StreamHandler<ServerResponse> respHandler) {
            this.respHandler = respHandler;
        }

        @Override
        public void onMessage(ClientRequest clientRequest) {
            if (clientRequest.getMsg().equals(CMD_TRIGGER_STREAM_FINISH)) {
                respHandler.onFinish();
            } else if (clientRequest.getMsg().equals(CMD_TRIGGER_STEAM_ERROR)) {
                respHandler.onException(new RuntimeException(ERROR_MSG));
            }
            else {
                respHandler.onMessage(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount()));
            }
        }

        @Override
        public void onFinish() {
            respHandler.onFinish();
        }

        @Override
        public void onException(Throwable throwable) {
            respHandler.onMessage(new ServerResponse("Received exception:" + throwable.getMessage(), -2));
            respHandler.onException(throwable);
            throwable.printStackTrace();
        }
    };
}
