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
package com.alipay.sofa.rpc.tracer.sofatracer.base;

import com.alipay.common.tracer.core.appender.TracerLogRootDeamon;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;

/**
 * AbstractTracerBase
 *
 * @author <a href=mailto:guanchao.ygc@antfin.com>GuanChao Yang</a>
 */
public abstract class AbstractTracerBase {

    protected final static String logDirectory = TracerLogRootDeamon.LOG_FILE_DIR;

    @BeforeClass
    public static void beforeClass() throws IOException {
        //清理tracelog
        cleanLogDirectory();
    }

    @AfterClass
    public static void afterClass() {
        //清理tracelog
    }

    /**
     * 清理日志文件夹
     */
    public static void cleanLogDirectory() {
        File traceLogDirectory = new File(logDirectory);
        if (!traceLogDirectory.exists()) {
            return;
        }
        FileUtils.cleanDirectory(traceLogDirectory);
    }
}
