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
package com.alipay.sofa.rpc.profile;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * Profile安全调用入口，隔离采集实现对RPC主链路的影响。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
public final class Profiles {

    private static final Logger     LOGGER = LoggerFactory.getLogger(Profiles.class);

    private static volatile Profile profile;

    private Profiles() {
    }

    /**
     * 安装Profile实例。
     *
     * @param newProfile Profile实例
     */
    public static void install(Profile newProfile) {
        profile = newProfile;
    }

    /**
     * 卸载当前Profile实例。
     */
    public static void uninstall() {
        profile = null;
    }

    /**
     * 当前是否需要采集数据。
     *
     * @return 是否启用
     */
    public static boolean isEnabled() {
        Profile current = profile;
        if (current == null) {
            return false;
        }
        try {
            return current.isEnabled();
        } catch (Throwable e) {
            warn("isEnabled", e);
            return false;
        }
    }

    public static void startRpc(SofaRequest request) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.startRpc(request);
            } catch (Throwable e) {
                warn("startRpc", e);
            }
        }
    }

    public static void clientBeforeSend(SofaRequest request) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.clientBeforeSend(request);
            } catch (Throwable e) {
                warn("clientBeforeSend", e);
            }
        }
    }

    public static void clientAfterSend(SofaRequest request) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.clientAfterSend(request);
            } catch (Throwable e) {
                warn("clientAfterSend", e);
            }
        }
    }

    public static void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.clientReceived(request, response, throwable);
            } catch (Throwable e) {
                warn("clientReceived", e);
            }
        }
    }

    public static void clientEnd(SofaRequest request, SofaResponse response, Throwable throwable) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.clientEnd(request, response, throwable);
            } catch (Throwable e) {
                warn("clientEnd", e);
            }
        }
    }

    public static void serverReceived(SofaRequest request) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.serverReceived(request);
            } catch (Throwable e) {
                warn("serverReceived", e);
            }
        }
    }

    public static void serverSend(SofaRequest request, SofaResponse response, Throwable throwable) {
        Profile current = enabledProfile();
        if (current != null) {
            try {
                current.serverSend(request, response, throwable);
            } catch (Throwable e) {
                warn("serverSend", e);
            }
        }
    }

    /**
     * 将阶段耗时记录到当前RPC调用。使用显式调用上下文可以支持异步线程切换，
     * 并避免多个异步调用共享线程级上下文时相互覆盖。
     *
     * @param context      RPC调用级内部上下文
     * @param phase        阶段标识
     * @param elapsedNanos 阶段耗时，单位纳秒
     */
    public static void recordPhase(RpcInternalContext context, String phase, long elapsedNanos) {
        Profile current = profile;
        if (current == null || context == null || phase == null || elapsedNanos < 0) {
            return;
        }
        try {
            current.recordPhase(context, phase, elapsedNanos);
        } catch (Throwable e) {
            warn("recordPhase", e);
        }
    }

    /**
     * 将阶段耗时记录到当前线程绑定的RPC调用。
     *
     * @param phase        阶段标识
     * @param elapsedNanos 阶段耗时，单位纳秒
     */
    public static void recordPhase(String phase, long elapsedNanos) {
        recordPhase(RpcInternalContext.peekContext(), phase, elapsedNanos);
    }

    public static void checkState() {
        Profile current = profile;
        if (current != null) {
            try {
                current.checkState();
            } catch (Throwable e) {
                warn("checkState", e);
            }
        }
    }

    private static Profile enabledProfile() {
        Profile current = profile;
        if (current == null) {
            return null;
        }
        try {
            return current.isEnabled() ? current : null;
        } catch (Throwable e) {
            warn("isEnabled", e);
            return null;
        }
    }

    private static void warn(String stage, Throwable throwable) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Profile callback failed at stage " + stage, throwable);
        }
    }
}
