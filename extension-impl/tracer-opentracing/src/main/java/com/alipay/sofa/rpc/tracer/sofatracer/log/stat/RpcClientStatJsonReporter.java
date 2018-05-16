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
import com.alipay.common.tracer.core.reporter.stat.model.StatKey;

/**
 * RpcClientStatReporter
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class RpcClientStatJsonReporter extends RpcClientStatReporter {

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

    private JsonStringBuilder buffer = new JsonStringBuilder();

    @Override
    public void print(StatKey statKey, long[] values) {
        if (this.isClosePrint.get()) {
            //关闭统计日志输出
            return;
        }
        print(statKey, values);
        //        buffer.reset();
        //        buffer.appendBegin("timestamp", Timestamp.currentTime());
        //        buffer.append("key", statKey.getKey());
        //        int i = 0;
        //        for (; i < values.length - 1; i++) {
        //            buffer.append(values[i]);
        //        }
        //        buffer.append(values[i]);
        //        buffer.append(statKey.getResult());
        //        buffer.appendEnd(statKey.getEnd());
        //        try {
        //            if (appender instanceof LoadTestAwareAppender) {
        //                ((LoadTestAwareAppender) appender).append(buffer.toString(), statKey.isLoadTest());
        //            } else {
        //                appender.append(buffer.toString());
        //            }
        //            // 这里强制刷一次
        //            appender.flush();
        //        } catch (Throwable t) {
        //            SelfLog.error("统计日志<" + statTracerName + ">输出异常", t);
        //        }
    }
}
