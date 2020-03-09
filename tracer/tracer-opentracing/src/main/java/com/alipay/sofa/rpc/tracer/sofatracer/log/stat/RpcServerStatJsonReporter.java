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
package com.alipay.sofa.rpc.tracer.sofatracer.log.stat;

import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.util.Map;

/**
 * RpcServerStatReporter
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcServerStatJsonReporter extends AbstractRpcStatJsonReporter {

    public RpcServerStatJsonReporter(String statTracerName, String rollingPolicy, String logReserveConfig) {
        super(statTracerName, rollingPolicy, logReserveConfig);
    }

    @Override
    public String getFromApp(Map<String, String> tagsWithStr) {
        return tagsWithStr.get(RpcSpanTags.REMOTE_APP);
    }

    @Override
    public String getToApp(Map<String, String> tagsWithStr) {
        return tagsWithStr.get(RpcSpanTags.LOCAL_APP);
    }

    @Override
    public String getZone(Map<String, String> tagsWithStr) {
        //服务端统计的是来源 zone
        return tagsWithStr.get(RpcSpanTags.REMOTE_ZONE);
    }

}
