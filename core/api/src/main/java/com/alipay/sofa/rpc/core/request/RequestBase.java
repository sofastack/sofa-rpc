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
package com.alipay.sofa.rpc.core.request;

import java.io.Serializable;

/**
 * Sofa RPC request base class
 * <p>
 * This class contains all data which RPC request object need
 * If want add other data you should extend this class
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>HongWei Yi</a>
 */
public abstract class RequestBase implements Serializable {

    private static final long  serialVersionUID = -7323141575870688636L;

    /**
     * 方法名
     */
    private String             methodName;

    /**
     * 方法参数签名invoke method arguments name
     */
    private String[]           methodArgSigs;

    /**
     * 方法参数值 invoke method arguments object
     */
    private transient Object[] methodArgs;

    /**
     * 服务唯一名称 traget service unique name
     */
    private String             targetServiceUniqueName;

    public String getMethodName() {
        return methodName;
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

    public String[] getMethodArgSigs() {
        return methodArgSigs;
    }

    public String getTargetServiceUniqueName() {
        return targetServiceUniqueName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    public void setMethodArgSigs(String[] methodArgSigs) {
        this.methodArgSigs = methodArgSigs;
    }

    public void setTargetServiceUniqueName(String targetServiceUniqueName) {
        this.targetServiceUniqueName = targetServiceUniqueName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("RequestBase[");
        sb.append("Service=").append(targetServiceUniqueName).append(", ");
        sb.append("Method=").append(methodName).append(", ");
        sb.append("Parameters=[");
        if (null != methodArgs) {
            for (Object arg : methodArgs) {
                sb.append(arg).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]]");
        return sb.toString();
    }

}
