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

import com.alipay.common.tracer.core.appender.builder.JsonStringBuilder;
import com.alipay.common.tracer.core.appender.file.LoadTestAwareAppender;
import com.alipay.common.tracer.core.appender.self.SelfLog;
import com.alipay.common.tracer.core.appender.self.Timestamp;
import com.alipay.common.tracer.core.reporter.stat.model.StatKey;
import com.alipay.common.tracer.core.reporter.stat.model.StatMapKey;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.util.Map;

/**
 * RpcClientStatReporter
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcClientStatJsonReporter extends AbstractRpcStatJsonReporter {

    private static JsonStringBuilder buffer = new JsonStringBuilder();

    /**
     * Instantiates a new Rpc client stat json reporter.
     *
     * @param statTracerName   the stat tracer name
     * @param rollingPolicy    the rolling policy
     * @param logReserveConfig the log reserve config
     */
    public RpcClientStatJsonReporter(String statTracerName, String rollingPolicy, String logReserveConfig) {
        super(statTracerName, rollingPolicy, logReserveConfig);
    }

    @Override
    public String getFromApp(Map<String, String> tagsWithStr) {
        return tagsWithStr.get(RpcSpanTags.LOCAL_APP);
    }

    @Override
    public String getToApp(Map<String, String> tagsWithStr) {
        return tagsWithStr.get(RpcSpanTags.REMOTE_APP);
    }

    @Override
    public String getZone(Map<String, String> tagsWithStr) {
        //客户端统计的是 targetZone
        return tagsWithStr.get(RpcSpanTags.REMOTE_ZONE);
    }

    @Override
    public void print(StatKey statKey, long[] values) {
        if (this.isClosePrint.get()) {
            //关闭统计日志输出
            return;
        }

        StatMapKey statMapKey = (StatMapKey) statKey;

        buffer.reset();
        buffer.appendBegin("time", Timestamp.currentTime());
        buffer.append("stat.key", this.statKeySplit(statMapKey));
        buffer.append("count", values[0]);
        buffer.append("total.cost.milliseconds", values[1]);
        buffer.append("success", statMapKey.getResult());
        buffer.appendEnd();
        try {
            if (appender instanceof LoadTestAwareAppender) {
                ((LoadTestAwareAppender) appender).append(buffer.toString(), statKey.isLoadTest());
            } else {
                appender.append(buffer.toString());
            }
            // 这里强制刷一次
            appender.flush();
        } catch (Throwable t) {
            SelfLog.error("统计日志<" + statTracerName + ">输出异常", t);
        }
    }

    private String statKeySplit(StatMapKey statKey) {
        JsonStringBuilder jsonBufferKey = new JsonStringBuilder();
        Map<String, String> keyMap = statKey.getKeyMap();
        jsonBufferKey.appendBegin();
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            jsonBufferKey.append(entry.getKey(), entry.getValue());
        }
        jsonBufferKey.appendEnd(false);
        return jsonBufferKey.toString();
    }
}
