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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extensible;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * 路由器：从一堆Provider中筛选出一堆Provider
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class Router {

    /**
     * 初始化
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public void init(ConsumerBootstrap consumerBootstrap) {
    }

    /**
     * 是否自动加载
     *
     * @param consumerBootstrap 调用对象
     * @return 是否加载本过滤器
     */
    public boolean needToLoad(ConsumerBootstrap consumerBootstrap) {
        return true;
    }

    /**
     * 筛选Provider
     *
     * @param request       本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providerInfos providers（<b>当前可用</b>的服务Provider列表）
     * @return 路由匹配的服务Provider列表
     */
    public abstract List<ProviderInfo> route(SofaRequest request, List<ProviderInfo> providerInfos);

    /**
     * 记录路由路径记录
     *
     * @param routerName 路由名字
     * @since 5.2.0
     */
    protected void recordRouterWay(String routerName) {
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext context = RpcInternalContext.getContext();
            String record = (String) context.getAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD);
            record = record == null ? routerName : record + ">" + routerName;
            context.setAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD, record);
        }
    }
}
