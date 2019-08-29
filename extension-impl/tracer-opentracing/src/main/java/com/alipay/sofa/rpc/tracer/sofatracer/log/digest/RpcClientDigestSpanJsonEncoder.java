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
package com.alipay.sofa.rpc.tracer.sofatracer.log.digest;

import com.alipay.common.tracer.core.appender.builder.JsonStringBuilder;
import com.alipay.common.tracer.core.appender.self.Timestamp;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.io.IOException;

/**
 * Encode RpcClientDigestSpan to json string
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcClientDigestSpanJsonEncoder extends AbstractRpcDigestSpanJsonEncoder {

    /**
     * for cocurrent consider ,we do not put it to parent class
     */
    private static JsonStringBuilder jsb = new JsonStringBuilder(true);

    @Override
    public String encode(SofaTracerSpan span) throws IOException {
        jsb.reset();
        //打印时间
        jsb.appendBegin(RpcSpanTags.TIMESTAMP, Timestamp.format(span.getEndTime()));
        //添加其他字段
        this.appendSlot(jsb, span);
        jsb.appendEnd();
        return jsb.toString();
    }
}
