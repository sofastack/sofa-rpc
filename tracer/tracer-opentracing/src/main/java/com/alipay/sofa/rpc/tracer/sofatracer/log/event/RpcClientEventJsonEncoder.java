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
package com.alipay.sofa.rpc.tracer.sofatracer.log.event;

import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.io.IOException;

/**
 * @author Even
 * @date 2025/3/19 19:33
 */
public class RpcClientEventJsonEncoder extends AbstractRpcEventJsonEncoder {

    @Override
    public String encode(SofaTracerSpan span) throws IOException {
        buffer.reset();
        buffer.appendBegin(RpcSpanTags.TIMESTAMP, span.getEventData().getTimestamp());
        appendSlot(span);
        buffer.appendEnd();
        return buffer.toString();
    }

}
