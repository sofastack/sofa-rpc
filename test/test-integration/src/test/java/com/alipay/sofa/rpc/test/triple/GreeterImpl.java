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
package com.alipay.sofa.rpc.test.triple;

import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterTriple;
import io.grpc.stub.StreamObserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GreeterImpl extends SofaGreeterTriple.GreeterImplBase {

    //Intentionally using unsupported format
    static final DateTimeFormatter[] datetimeFormatter = new DateTimeFormatter[] { DateTimeFormatter.ISO_DATE_TIME,
                                                       DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                                       DateTimeFormatter.BASIC_ISO_DATE };

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        HelloRequest.DateTime reqDateTime = req.getDateTime();
        int i = 0;
        try {
            i = Integer.parseInt(reqDateTime.getTime());
        } catch (Exception e) {
            //TODO: handle exception
        }
        LocalDateTime dt = LocalDateTime.now();
        String dtStr = dt.format(datetimeFormatter[i % datetimeFormatter.length]);
        HelloRequest.DateTime rplyDateTime = HelloRequest.DateTime.newBuilder(reqDateTime)
            .setDate(dtStr).build();
        HelloReply reply = HelloReply.newBuilder()
            .setMessage("Hello " + req.getName())
            .setDateTime(rplyDateTime)
            .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}