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
package com.alipay.sofa.rpc.proxy;

import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;

import java.lang.reflect.Method;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class TestInvoker implements Invoker {

    private TestInterfaceImpl testInterface = new TestInterfaceImpl();
    private SofaRequest       request;

    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        this.request = request;
        if ("throwRPC".equals(request.getMethodName())) {
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, "xxxx");
        }
        SofaResponse response = new SofaResponse();
        try {
            Method method = TestInterface.class.getMethod(request.getMethodName(),
                ClassTypeUtils.getClasses(request.getMethodArgSigs()));
            Object ret = method.invoke(testInterface, request.getMethodArgs());
            response = new SofaResponse();
            response.setAppResponse(ret);
        } catch (Exception e) {
            response.setErrorMsg(e.getMessage());
        }
        return response;
    }

    @Override
    public String toString() {
        return "com.xxx:1.0:xsdsd@123";
    }

    public SofaRequest getRequest() {
        return request;
    }
}
