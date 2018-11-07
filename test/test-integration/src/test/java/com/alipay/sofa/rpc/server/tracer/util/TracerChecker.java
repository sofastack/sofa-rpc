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
package com.alipay.sofa.rpc.server.tracer.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bystander
 * @version $Id: TracerChecker.java, v 0.1 2018年11月06日 10:20 AM bystander Exp $
 */
public class TracerChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TracerChecker.class);

    /**
     * readTracerDigest all rpc client and rpc server digest to jsonObject
     *
     * @param file
     * @return
     */
    public static List<JSONObject> readTracerDigest(File file) {
        List<JSONObject> JSONObjects = new ArrayList<JSONObject>();
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new FileReader(file);
            bufferedReader = new BufferedReader(reader);
            String lineText = null;
            while ((lineText = bufferedReader.readLine()) != null) {
                //this is json format now
                JSONObjects.add(JSON.parseObject(lineText));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return JSONObjects;
    }

    public static String extractField(JSONObject jsonObject, String fieldName) {

        String value;
        if (jsonObject != null) {
            value = jsonObject.getString(fieldName);
        } else {
            value = "";
        }
        return value;
    }

    public static List<String> extractFields(List<JSONObject> jsonObjects, String fieldName) {

        List<String> result = new ArrayList<String>();
        if (CommonUtils.isEmpty(jsonObjects)) {
            return result;
        } else {
            for (JSONObject jsonObject : jsonObjects) {
                result.add(extractField(jsonObject, fieldName));
            }
        }

        return result;
    }

    /**
     * @param jsonObject
     * @param type
     * @param protocol
     * @return
     */
    public static boolean validateTracerDigest(JSONObject jsonObject, String type, String protocol) {

        System.out.println("validateField,value=" + jsonObject.toJSONString());

        String tracerId = jsonObject.getString(RpcSpanTags.TRACERID);
        String spanId = jsonObject.getString(RpcSpanTags.SPANID);
        String timeStamp = jsonObject.getString(RpcSpanTags.TIMESTAMP);
        String service = jsonObject.getString(RpcSpanTags.SERVICE);
        String method = jsonObject.getString(RpcSpanTags.METHOD);
        String threadName = jsonObject.getString(RpcSpanTags.CURRENT_THREAD_NAME);
        String localApp = jsonObject.getString(RpcSpanTags.LOCAL_APP);
        String router = jsonObject.getString(RpcSpanTags.ROUTE_RECORD);
        String resultCode = jsonObject.getString(RpcSpanTags.RESULT_CODE);
        String baggage = jsonObject.getString(RpcSpanTags.BAGGAGE);
        String currentProtocol = jsonObject.getString(RpcSpanTags.PROTOCOL);
        String bizTime = jsonObject.getString(RpcSpanTags.SERVER_BIZ_TIME);
        String reqSize = jsonObject.getString(RpcSpanTags.REQ_SIZE);
        String respSize = jsonObject.getString(RpcSpanTags.RESP_SIZE);
        String remoteIp = jsonObject.getString(RpcSpanTags.REMOTE_IP);
        String localIp = jsonObject.getString(RpcSpanTags.LOCAL_IP);
        String localPort = jsonObject.getString(RpcSpanTags.LOCAL_PORT);
        String reqSerializeTime = jsonObject.getString(RpcSpanTags.REQ_SERIALIZE_TIME);
        String respDeserializeTime = jsonObject.getString(RpcSpanTags.RESP_DESERIALIZE_TIME);
        String reqDeserializeTime = jsonObject.getString(RpcSpanTags.REQ_DESERIALIZE_TIME);
        String respSerializeTime = jsonObject.getString(RpcSpanTags.RESP_SERIALIZE_TIME);
        String remoteApp = jsonObject.getString(RpcSpanTags.REMOTE_APP);
        String clientElapseTime = jsonObject.getString(RpcSpanTags.CLIENT_ELAPSE_TIME);
        String clientConnTime = jsonObject.getString(RpcSpanTags.CLIENT_CONN_TIME);
        String invokeType = jsonObject.getString(RpcSpanTags.INVOKE_TYPE);
        String serverWaitTime = jsonObject.getString(RpcSpanTags.SERVER_THREAD_POOL_WAIT_TIME);

        if ("client".equalsIgnoreCase(type)) {

            if (RpcConstants.PROTOCOL_TYPE_BOLT.equalsIgnoreCase(protocol)) {
                return validateField(tracerId, spanId, timeStamp, service, router, resultCode, method, localApp,
                    threadName, invokeType, currentProtocol, respSize, localIp, localPort, reqSerializeTime,
                    respDeserializeTime, remoteApp, clientElapseTime, clientConnTime, remoteIp);

            } else if (RpcConstants.PROTOCOL_TYPE_REST.equalsIgnoreCase(protocol)) {
                return validateField(tracerId, spanId, timeStamp, service, router, resultCode, method, localApp,
                    threadName, invokeType, currentProtocol, localPort, remoteIp, localIp, clientElapseTime);

            } else {
                return false;
            }

        } else if ("server".equalsIgnoreCase(type)) {
            if (RpcConstants.PROTOCOL_TYPE_BOLT.equalsIgnoreCase(protocol)) {
                return validateField(tracerId, spanId, timeStamp, service, resultCode, method,
                    threadName, bizTime, reqSize, respSize, invokeType, remoteIp, currentProtocol, remoteApp,
                    reqDeserializeTime, respSerializeTime, serverWaitTime);

            } else if (RpcConstants.PROTOCOL_TYPE_REST.equalsIgnoreCase(protocol)) {
                return validateField(tracerId, spanId, timeStamp, service, resultCode, method,
                    threadName, bizTime, reqSize, respSize, invokeType, remoteIp, currentProtocol, remoteApp);
            } else {
                return false;
            }

        } else {
            return false;
        }
    }

    private static boolean validateField(String... fileds) {

        System.out.println("validateField,value=" + StringUtils.join(fileds, ","));

        LOGGER.info("validateField,value=" + StringUtils.join(fileds, ","));

        for (String field : fileds) {
            if (StringUtils.isEmpty(field)) {
                return false;
            }
        }

        return true;
    }
}