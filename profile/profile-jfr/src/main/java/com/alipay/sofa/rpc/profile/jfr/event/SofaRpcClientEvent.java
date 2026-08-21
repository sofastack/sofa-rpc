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
package com.alipay.sofa.rpc.profile.jfr.event;

import com.alipay.sofa.rpc.profile.jfr.JfrEventNames;
import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

/**
 * 客户端RPC调用JFR事件。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
@Name(JfrEventNames.CLIENT_INVOCATION)
@Label("SOFA RPC Client Invocation")
@Category({ "SOFA RPC", "Invocation" })
@Description("Client-side SOFA RPC invocation and its phase durations")
@StackTrace(false)
public class SofaRpcClientEvent extends Event {

    @Label("Profile ID")
    public String profileId                   = "";

    @Label("Service")
    public String service                     = "";

    @Label("Method")
    public String method                      = "";

    @Label("Source Application")
    public String sourceApp                   = "";

    @Label("Target Application")
    public String targetApp                   = "";

    @Label("Protocol")
    public String protocol                    = "";

    @Label("Invocation Type")
    public String invokeType                  = "";

    @Label("Local Address")
    public String localAddress                = "";

    @Label("Remote Address")
    public String remoteAddress               = "";

    @Label("Result")
    public String result                      = "INCOMPLETE";

    @Label("Error Class")
    public String errorClass                  = "";

    @Label("SOFA RPC Error Code")
    public int    errorCode                   = -1;

    @Label("Invocation Count")
    public int    invokeCount                 = 1;

    @Label("Request Size")
    @DataAmount(DataAmount.BYTES)
    public long   requestSize                 = -1L;

    @Label("Response Size")
    @DataAmount(DataAmount.BYTES)
    public long   responseSize                = -1L;

    @Label("First Streaming Response")
    @Timespan(Timespan.NANOSECONDS)
    public long   firstResponseTime           = -1L;

    @Label("Router")
    @Timespan(Timespan.NANOSECONDS)
    public long   routerTime                  = -1L;

    @Label("Connection Creation")
    @Timespan(Timespan.NANOSECONDS)
    public long   connectionTime              = -1L;

    @Label("Client Filter")
    @Timespan(Timespan.NANOSECONDS)
    public long   clientFilterTime            = -1L;

    @Label("Load Balancer")
    @Timespan(Timespan.NANOSECONDS)
    public long   loadBalancerTime            = -1L;

    @Label("Request Serialization")
    @Timespan(Timespan.NANOSECONDS)
    public long   requestSerializationTime    = -1L;

    @Label("Response Deserialization")
    @Timespan(Timespan.NANOSECONDS)
    public long   responseDeserializationTime = -1L;
}
