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
package com.alipay.sofa.rpc.test.exception;

import com.alipay.sofa.rpc.core.exception.SofaBizRetryException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

/**
 *
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class TestExceptionServiceImpl implements TestExceptionService {
    @Override
    public String throwRuntimeException() {
        throw new RuntimeException("RuntimeException");
    }

    @Override
    public String throwException() throws Exception {
        throw new Exception("Exception");
    }

    @Override
    public String throwSofaException() throws SofaRpcException {
        throw new SofaRpcException("SofaRpcException");
    }

    @Override
    public String throwDeclaredException() throws TestException {
        throw new TestException("TestException");
    }

    @Override
    public void throwDeclaredExceptionWithoutReturn() throws TestException {
        throw new TestException("DeclaredExceptionWithoutReturn");
    }

    @Override
    public void throwSofaBizRetryException() throws SofaBizRetryException {
        System.out.println("do sofaBizRetry Service");
        throw new SofaBizRetryException(new Exception("biz retry"));
    }
}
