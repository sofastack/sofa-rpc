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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.context.AsyncRuntime;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Simply event bus for internal event transport.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class EventBus {

    private static final Logger  LOGGER           = LoggerFactory.getLogger(EventBus.class);

    /**
     * 是否允许携带上下文附件，关闭后只能传递"."开头的key，"_" 开头的Key将不被保持和传递。<br>
     * 在性能测试等场景可能关闭此传递功能。
     */
    private static final boolean EVENT_BUS_ENABLE = RpcConfigs.getBooleanValue(RpcOptions.EVENT_BUS_ENABLE);

    /**
     * 是否开启事件总线功能
     *
     * @return 是否开启事件总线功能
     */
    public static boolean isEnable() {
        return EVENT_BUS_ENABLE;
    }

    /**
     * 是否开启事件总线功能
     *
     * @param eventClass 事件类型
     * @return 是否开启事件总线功能
     */
    public static boolean isEnable(Class<? extends Event> eventClass) {
        return EVENT_BUS_ENABLE && CommonUtils.isNotEmpty(SUBSCRIBER_MAP.get(eventClass));
    }

    /**
     * 某中事件的订阅者
     */

    private final static ConcurrentMap<Class<? extends Event>, CopyOnWriteArraySet<Subscriber>> SUBSCRIBER_MAP = new ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArraySet<Subscriber>>();

    /**
     * 注册一个订阅者
     *
     * @param eventClass 事件类型
     * @param subscriber 订阅者
     */
    public static void register(Class<? extends Event> eventClass, Subscriber subscriber) {
        CopyOnWriteArraySet<Subscriber> set = SUBSCRIBER_MAP.get(eventClass);
        if (set == null) {
            set = new CopyOnWriteArraySet<Subscriber>();
            CopyOnWriteArraySet<Subscriber> old = SUBSCRIBER_MAP.putIfAbsent(eventClass, set);
            if (old != null) {
                set = old;
            }
        }
        set.add(subscriber);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Register subscriber: {} of event: {}.", subscriber, eventClass);
        }
    }

    /**
     * 反注册一个订阅者
     *
     * @param eventClass 事件类型
     * @param subscriber 订阅者
     */
    public static void unRegister(Class<? extends Event> eventClass, Subscriber subscriber) {
        CopyOnWriteArraySet<Subscriber> set = SUBSCRIBER_MAP.get(eventClass);
        if (set != null) {
            set.remove(subscriber);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("UnRegister subscriber: {} of event: {}.", subscriber, eventClass);
            }
        }
    }

    /**
     * 给事件总线中丢一个事件
     *
     * @param event 事件
     */
    public static void post(final Event event) {
        if (!isEnable()) {
            return;
        }
        CopyOnWriteArraySet<Subscriber> subscribers = SUBSCRIBER_MAP.get(event.getClass());
        if (CommonUtils.isNotEmpty(subscribers)) {
            for (final Subscriber subscriber : subscribers) {
                if (subscriber.isSync()) {
                    handleEvent(subscriber, event);
                } else { // 异步
                    final RpcInternalContext context = RpcInternalContext.peekContext();
                    final ThreadPoolExecutor asyncThreadPool = AsyncRuntime.getAsyncThreadPool();
                    try {
                        asyncThreadPool.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        RpcInternalContext.setContext(context);
                                        handleEvent(subscriber, event);
                                    } catch (Exception e) {
                                        RpcInternalContext.removeContext();
                                    }
                                }
                            });
                    } catch (RejectedExecutionException e) {
                        LOGGER
                            .warn("This queue is full when post event to async execute, queue size is " +
                                asyncThreadPool.getQueue().size() +
                                ", please optimize this async thread pool of eventbus.");
                    }
                }
            }
        }
    }

    private static void handleEvent(final Subscriber subscriber, final Event event) {
        try {
            subscriber.onEvent(event);
        } catch (Throwable e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Handle " + event.getClass() + " error", e);
            }
        }
    }
}
