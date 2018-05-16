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
package com.alipay.sofa.rpc.tracer.sofatracer;

import com.alipay.common.tracer.core.appender.TracerLogRootDeamon;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.tracer.sofatracer.log.type.RpcTracerLogEnum;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * TracerCountTest
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public class TracerCountTest extends AbstractTracerBase {

    private static final int    REF_COUNT    = 10;

    private final static Logger LOGGER       = LoggerFactory.getLogger(TracerCountTest.class);

    private static String       logDirectory = TracerLogRootDeamon.LOG_FILE_DIR;

    @Before
    public void before() throws Exception {

        LOGGER.info("current log path:" + logDirectory);
        reflectSetNewTracer();
        this.pulishService();
    }

    public void pulishService() {
        //server publish service
        ServerConfig serverConfig = new ServerConfig()
            .setHost("0.0.0.0")
            .setPort(22000)
            .setDaemon(false);

        ProviderConfig<TracerService> providerConfig = new ProviderConfig<TracerService>()
            .setInterfaceId(TracerService.class.getName())
            .setRef(new TracerServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();
        System.err.println("PID :" + RpcRuntimeContext.PID);
    }

    @Test
    public void clientServerTest() throws Exception {
        //client reference service
        ConsumerConfig<TracerService> consumerConfig = new ConsumerConfig<TracerService>()
            .setInterfaceId(TracerService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22000")
            .setRegister(false)
            .setTimeout(3000);
        TracerService tracerService = consumerConfig.refer();
        try {
            for (int i = 0; i < REF_COUNT; i++) {
                try {
                    String s = tracerService.sayTracer("xxx" + i, i);
                    LOGGER.info("Result : {}", s);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                Thread.sleep(100);
            }

        } catch (Exception e) {
            LOGGER.error("", e);
        }
        try {
            Thread.sleep(2000); // 等待写完
        } finally {
        }
        //assert digest
        List clientDigestLogs = FileUtils.readLines(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName()));
        assertEquals(REF_COUNT, clientDigestLogs.size());

        List serverDigestLogs = FileUtils.readLines(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_CLIENT_DIGEST.getDefaultLogName()));
        assertEquals(REF_COUNT, serverDigestLogs.size());

        //统计日志
        Thread.sleep(2 * 1000); // 等待一分钟统计日志
        //assert statistics
        List clientStatLogs = FileUtils.readLines(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_CLIENT_STAT.getDefaultLogName()));
        assertEquals(1, clientStatLogs.size());

        List serverStattLogs = FileUtils.readLines(new File(logDirectory + File.separator
            + RpcTracerLogEnum.RPC_SERVER_STAT.getDefaultLogName()));
        assertEquals(1, serverStattLogs.size());

    }
}
