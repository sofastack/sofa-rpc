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
     * Method name
     */
    private String             methodName;

    /**
     * Argument type strings of method
     */
    private String[]           methodArgSigs;

    /**
     * Argument values of method
     */
    private transient Object[] methodArgs;

    /**
     * Target service unique name, contains interfaceName, uniqueId and etc.
     */
    private String             targetServiceUniqueName;

    /**
     * Gets method name.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Get method args object [ ].
     *
     * @return the object [ ]
     */
    public Object[] getMethodArgs() {
        return methodArgs;
    }

    /**
     * Get method arg sigs string [ ].
     *
     * @return the string [ ]
     */
    public String[] getMethodArgSigs() {
        return methodArgSigs;
    }

    /**
     * Gets target service unique name.
     *
     * @return the target service unique name
     */
    public String getTargetServiceUniqueName() {
        return targetServiceUniqueName;
    }

    /**
     * Sets method name.
     *
     * @param methodName the method name
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Sets method args.
     *
     * @param methodArgs the method args
     */
    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    /**
     * Sets method arg sigs.
     *
     * @param methodArgSigs the method arg sigs
     */
    public void setMethodArgSigs(String[] methodArgSigs) {
        this.methodArgSigs = methodArgSigs;
    }

    /**
     * Sets target service unique name.
     *
     * @param targetServiceUniqueName the target service unique name
     */
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
