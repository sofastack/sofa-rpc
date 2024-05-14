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
package com.alipay.sofa.rpc.test.triple.stream;

import com.alipay.sofa.rpc.transport.SofaStreamObserver;

public interface HelloService {

    String CMD_TRIGGER_STREAM_FINISH = "finish";

    String CMD_TRIGGER_STREAM_ERROR  = "error";

    String ERROR_MSG                 = "error msg";

    SofaStreamObserver<ClientRequest> sayHelloBiStream(SofaStreamObserver<ServerResponse> sofaStreamObserver);

    void sayHelloServerStream(ClientRequest clientRequest, SofaStreamObserver<ServerResponse> sofaStreamObserver);
}
