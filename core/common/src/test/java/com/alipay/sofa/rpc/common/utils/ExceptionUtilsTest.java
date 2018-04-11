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

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ExceptionUtilsTest {
    @Test
    public void buildRuntime() throws Exception {
        SofaRpcRuntimeException exception = ExceptionUtils.buildRuntime("xxx111", "222");
        Assert.assertTrue(exception.getMessage().contains("xxx111"));
        Assert.assertTrue(exception.getMessage().contains("222"));
    }

    @Test
    public void buildRuntime1() throws Exception {
        SofaRpcRuntimeException exception = ExceptionUtils.buildRuntime("xxx111", "222", "yyy");
        Assert.assertTrue(exception.getMessage().contains("xxx111"));
        Assert.assertTrue(exception.getMessage().contains("222"));
        Assert.assertTrue(exception.getMessage().contains("yyy"));
    }

    @Test
    public void isServerException() throws Exception {
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, "111");
        Assert.assertTrue(ExceptionUtils.isServerException(exception));
        exception = new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT, "111");
        Assert.assertFalse(ExceptionUtils.isServerException(exception));
    }

    @Test
    public void isClientException() throws Exception {
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, "111");
        Assert.assertFalse(ExceptionUtils.isClientException(exception));
        exception = new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT, "111");
        Assert.assertTrue(ExceptionUtils.isClientException(exception));
    }

    @Test
    public void testToString() throws Exception {
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, "111");
        String string = ExceptionUtils.toString(exception);
        Assert.assertNotNull(string);
        Pattern pattern = Pattern.compile("at");
        Matcher matcher = pattern.matcher(string);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        Assert.assertTrue(count > 1);
    }

    @Test
    public void toShortString() throws Exception {
        SofaRpcException exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, "111");
        String string = ExceptionUtils.toShortString(exception, 1);
        Assert.assertNotNull(string);
        Pattern pattern = Pattern.compile("at");
        Matcher matcher = pattern.matcher(string);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        Assert.assertTrue(count == 1);
    }

}