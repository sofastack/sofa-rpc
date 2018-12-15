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
package com.alipay.sofa.rpc.registry.local;

import com.alipay.sofa.rpc.common.utils.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class LocalRegistryHelperTest {

    private static String filePath = System.getProperty("user.home") + File.separator
                                       + "localFileTest"
                                       + new Random().nextInt(1000);

    @Test
    public void testModify() {

        final File file = new File(filePath);
        FileUtils.cleanDirectory(file);

        try {
            FileUtils.string2File(file, "a");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String oldDigest = LocalRegistryHelper.calMD5Checksum(filePath);

        try {
            FileUtils.string2File(file, "b");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String newDigest = LocalRegistryHelper.calMD5Checksum(filePath);

        Assert.assertNotSame("digest不能一样", oldDigest, newDigest);
        Assert.assertTrue(LocalRegistryHelper.checkModified(filePath, oldDigest));
        Assert.assertFalse(LocalRegistryHelper.checkModified(filePath, newDigest));
    }

    @Test
    public void testNotModify() {

        final File file = new File(filePath);
        FileUtils.cleanDirectory(file);

        try {
            FileUtils.string2File(file, "a");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String oldDigest = LocalRegistryHelper.calMD5Checksum(filePath);

        String newDigest = LocalRegistryHelper.calMD5Checksum(filePath);

        Assert.assertEquals("digest不一样", oldDigest, newDigest);
        Assert.assertFalse(LocalRegistryHelper.checkModified(filePath, oldDigest));
    }
}