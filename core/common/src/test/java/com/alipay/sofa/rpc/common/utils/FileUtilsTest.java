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
package com.alipay.sofa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FileUtilsTest {
    @Test
    public void getBaseDirName() throws Exception {
        Assert.assertNotNull(FileUtils.getBaseDirName());
    }

    @Test
    public void getUserHomeDir() throws Exception {
    }

    @Test
    public void file2String() throws Exception {
    }

    @Test
    public void file2String1() throws Exception {
    }

    @Test
    public void readLines() throws Exception {
    }

    @Test
    public void string2File() throws Exception {
    }

    @Test
    public void cleanDirectory() throws Exception {
        String filePath = System.getProperty("java.io.tmpdir") + File.separator
            + "FileTest" + 1;
        FileUtils.string2File(new File(filePath, "xx.tmp"), "helloworld!");
        Assert.assertTrue(new File(filePath, "xx.tmp").exists());

        String ct = FileUtils.file2String(new File(filePath, "xx.tmp"));
        Assert.assertTrue(ct.equals("helloworld!"));

        List<String> datas = FileUtils.readLines(new File(filePath, "xx.tmp"));
        Assert.assertTrue(datas.size() == 1);

        FileUtils.cleanDirectory(new File(filePath));
        Assert.assertFalse(new File(filePath).exists());
    }

}