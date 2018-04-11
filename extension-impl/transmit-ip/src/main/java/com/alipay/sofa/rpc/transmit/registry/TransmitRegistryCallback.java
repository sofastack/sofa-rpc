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
package com.alipay.sofa.rpc.transmit.registry;

import java.util.List;

/**
 * 订阅回调接口
 */
public interface TransmitRegistryCallback {
    /**
     * Handle the data of full amount from registry callback
     *
     *
     * @param dataId Data ID
     * @param strings Data list
     */
    void handleData(String dataId, List<String> strings);

    /**
     * Handle the data of add from registry callback
     * @param dataId
     * @param add
     */
    void addData(String dataId, String add);

    /**
     * Handle the data of delete from registry callback
     * @param dataId
     * @param delete
     */
    void deleteData(String dataId, String delete);

    /**
     * set the data of full amount from registry callback
     *
     * @param dataId
     * @param strings
     */
    void setData(String dataId, List<String> strings);

}