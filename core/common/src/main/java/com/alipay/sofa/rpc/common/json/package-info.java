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
package com.alipay.sofa.rpc.common.json;

/*
 * 本包为简单的JSON解析类，性能一般，支持格式也有限，主要用于配置文件解析。
 * 建议只在配置加载等场景中使用。
 * 但是有一个特别的地方就是支持注释的json文档。
 * <p>
 * 自定义对象通过先转为Map再转为对象：JSON<-> Map <-> Object
 */