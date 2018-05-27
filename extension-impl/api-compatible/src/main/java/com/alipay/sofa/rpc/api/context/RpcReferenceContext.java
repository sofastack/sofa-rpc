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
package com.alipay.sofa.rpc.api.context;

/**
 * RPC Reference Context
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>hongwei.yhw</a>
 */
public class RpcReferenceContext {

    protected String         traceId;
    protected String         rpcId;

    /** service interface */
    protected String         interfaceName;
    /** service invoke method */
    protected String         methodName;
    /** uniqueId */
    protected String         uniqueId;
    /** unique service name */
    protected String         serviceName;
    /** is generic service */
    protected boolean        isGeneric;

    /** target appName */
    protected String         targetAppName;
    /** target url */
    protected String         targetUrl;

    /** RPC protocol, such as TR */
    protected String         protocol;
    /** RPC invoke type, such as sync, oneway */
    protected String         invokeType;
    /** RPC route trace
     for example: TURL>CFS>RDM, it indicate route trace is: test-url to config server to random select */
    protected String         routeRecord;

    protected long           connEstablishedSpan;

    /** cost time (ms) */
    protected long           costTime;
    /** result code
     * 00: success, 01: application exception, 02: framework exception, 03: timeout exception, 04: route exception */
    protected ResultCodeEnum resultCode;

    /** request size */
    protected long           requestSize;

    /** response size */
    protected long           responseSize;

    /** client ip */
    String                   clientIP;
    /** rpc客户端端口号 */
    int                      clientPort;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    public void setGeneric(boolean generic) {
        this.isGeneric = generic;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRpcId() {
        return rpcId;
    }

    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }

    public String getTargetAppName() {
        return targetAppName;
    }

    public void setTargetAppName(String targetAppName) {
        this.targetAppName = targetAppName;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getInvokeType() {
        return invokeType;
    }

    public void setInvokeType(String invokeType) {
        this.invokeType = invokeType;
    }

    public String getRouteRecord() {
        return routeRecord;
    }

    public void setRouteRecord(String routeRecord) {
        this.routeRecord = routeRecord;
    }

    public long getConnEstablishedSpan() {
        return connEstablishedSpan;
    }

    public void setConnEstablishedSpan(long connEstablishedSpan) {
        this.connEstablishedSpan = connEstablishedSpan;
    }

    public long getCostTime() {
        return costTime;
    }

    public void setCostTime(long costTime) {
        this.costTime = costTime;
    }

    public ResultCodeEnum getResultCode() {
        return resultCode;
    }

    public void setResultCode(ResultCodeEnum resultCode) {
        this.resultCode = resultCode;
    }

    public long getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(long requestSize) {
        this.requestSize = requestSize;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    @Deprecated
    public String getClinetIP() {
        return clientIP;
    }

    @Deprecated
    public void setClinetIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }
}
