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
package com.alipay.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.DefaultConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * Consumer bootstrap for grpc
 *
 * @author <a href=mailto:yqluan@gmail.com>Yanqiang Oliver Luan (neokidd)</a>
 */
@Extension("grpc")
public class GrpcConsumerBootstrap<T> extends DefaultConsumerBootstrap<T> {
    public GrpcConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        super(consumerConfig);
    }
}
