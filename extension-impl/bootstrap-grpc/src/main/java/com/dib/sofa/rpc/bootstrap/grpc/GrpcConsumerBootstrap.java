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
package com.dib.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.bootstrap.DefaultConsumerBootstrap;
import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ClusterFactory;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.Proxy;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.LoaderClassPath;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.message.MessageBuilder;

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;


/**
 * Consumer bootstrap for grpc 
 *
 * @author <a href=mailto:luanyanqiang@dibgroup.cn>Luan Yanqiang</a>
 */
@Extension("grpc")
public class GrpcConsumerBootstrap<T> extends DefaultConsumerBootstrap<T> {

    /**
     * 代理实现类
     */
    protected transient volatile T                              proxyIns;

    /**
    * 调用类
    */
    // protected transient volatile Cluster                        cluster;

    /**
     * 计数器
     */
    // protected transient volatile CountDownLatch                 respondRegistries;

    /**
     * 发布的调用者配置（含计数器）
     */
    protected final static ConcurrentMap<String, AtomicInteger> REFERRED_KEYS = new ConcurrentHashMap<String, AtomicInteger>();

    private ManagedChannel channel;

    String host;
    int port;
    private final static Logger LOGGER = LoggerFactory.getLogger(GrpcConsumerBootstrap.class);

    /**
     * 构造函数
     
     * @param consumerConfig 服务消费者配置
     */
    protected GrpcConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        super(consumerConfig);
        // try {
        //     URL url = new URL(consumerConfig.getDirectUrl());
        //     host = url.getHost();
        //     port = url.getPort();
        //     // if ("".equals(host)) {
        //     host = "127.0.0.1";
        //     // }
        //     // if (port == 0) {
        //     port = 50052;
        //     // }
        // } catch (Exception e) {
        //     //TODO: handle exception
        //     LOGGER.error("illegal direct url: {}", consumerConfig.getDirectUrl());
        //     host = "127.0.0.1";
        //     port = 50052;
        // }
        // host = "127.0.0.1";
        // port = 50052;
        // channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    /**
     * Refer t.
     *
     * @return the t
     * @throws SofaRpcRuntimeException the init error exception
     */
    @Override
    public synchronized T refer() {
        if (proxyIns != null) {
            return proxyIns;
        }

        ProxyObject proxyObject = null;

        String key = consumerConfig.buildKey();
        String appName = consumerConfig.getAppName();
        // 检查参数
        checkParameters();
        // 提前检查接口类
        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "Refer consumer config : {} with bean id {}", key, consumerConfig.getId());
        }

        // 注意同一interface，同一tags，同一protocol情况
        AtomicInteger cnt = REFERRED_KEYS.get(key); // 计数器
        if (cnt == null) { // 没有发布过
            cnt = CommonUtils.putToConcurrentMap(REFERRED_KEYS, key, new AtomicInteger(0));
        }
        int c = cnt.incrementAndGet();
        int maxProxyCount = consumerConfig.getRepeatedReferLimit();
        if (maxProxyCount > 0) {
            if (c > maxProxyCount) {
                cnt.decrementAndGet();
                // 超过最大数量，直接抛出异常
                throw new SofaRpcRuntimeException("Duplicate consumer config with key " + key
                    + " has been referred more than " + maxProxyCount + " times!"
                    + " Maybe it's wrong config, please check it."
                    + " Ignore this if you did that on purpose!");
            } else if (c > 1) {
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, "Duplicate consumer config with key {} has been referred!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!", key);
                }
            }
        }

        try {
            // build cluster
            cluster = ClusterFactory.getCluster(this);
            // build listeners
            consumerConfig.setConfigListener(buildConfigListener(this));
            consumerConfig.setProviderInfoListener(buildProviderInfoListener(this));
            // init cluster
            cluster.init();
            // 构造Invoker对象（执行链）
            proxyInvoker = buildClientProxyInvoker(this);
            // 创建代理类
            // proxyIns = (T) ProxyFactory.buildProxy(consumerConfig.getProxy(), consumerConfig.getProxyClass(),
            // proxyInvoker);

            if (cluster.getAddressHolder().getProviderInfos("").size() != 0) {
                host = cluster.getAddressHolder().getProviderInfos("").get(0).getHost();
                port = cluster.getAddressHolder().getProviderInfos("").get(0).getPort();
            } else {
                host = "localhost";
                port = 50052;
            }
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

            try {
                Method newBlockingChannel = Class
                    .forName("io.grpc.examples.helloworld.GreeterGrpc"/*consumerConfig.getInterfaceName()*/)
                    .getDeclaredMethod("newBlockingStub", Channel.class);
                newBlockingChannel.setAccessible(true);
                proxyIns = (T) newBlockingChannel.invoke(null, channel);

            } catch (ClassNotFoundException e) {
                LOGGER.error("ClassNotFoundException");
                throw e;
            } catch (IllegalAccessException e) {
                LOGGER.error("IllegalAccessException");
                throw e;
            } catch (NoSuchMethodException e) {
                LOGGER.error("NoSuchMethodException");
                throw e;
            } catch (InvocationTargetException e) {
                LOGGER.error("InvocationTargetException");
                throw e;
            } catch (IllegalArgumentException e) {
                LOGGER.error("IllegalArgumentException");
                throw e;
            } finally {

            }

            // 创建代理类
            // proxyIns = buildProxy();

            Class<ProxyObject> proxyClass = null;
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(Class.forName("io.grpc.examples.helloworld.GreeterGrpc$GreeterBlockingStub"));
            proxyClass = proxyFactory.createClass();
            try {
                // proxyObject = proxyClass.newInstance();
                proxyObject = (ProxyObject) proxyClass.getConstructors()[0].newInstance(channel, proxyIns);
                channel.shutdown();
                proxyObject.setHandler(new MethodHandler() {
                    @Override
                    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args)
                        throws Throwable {
                        SofaRequest sofaRequest = MessageBuilder.buildSofaRequest(thisMethod.getDeclaringClass(),
                            thisMethod, thisMethod.getParameterTypes(), args);

                        SofaResponse sofaResponse = proxyInvoker.invoke(sofaRequest);
                        if (sofaResponse.isError()) {
                            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, sofaResponse.getErrorMsg());
                        }
                        Object appResponse = sofaResponse.getAppResponse();
                        if (appResponse instanceof Throwable) {
                            throw (Throwable) appResponse;
                        }
                        return sofaResponse.getAppResponse();
                    }
                });
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("", e);
            }

        } catch (Exception e) {
            if (cluster != null) {
                cluster.destroy();
                cluster = null;
            }
            consumerConfig.setConfigListener(null);
            consumerConfig.setProviderInfoListener(null);
            cnt.decrementAndGet(); // 发布失败不计数
            if (e instanceof SofaRpcRuntimeException) {
                throw (SofaRpcRuntimeException) e;
            } else {
                throw new SofaRpcRuntimeException("Build consumer proxy error!", e);
            }
        }

        if (consumerConfig.getOnAvailable() != null && cluster != null) {
            cluster.checkStateChange(false); // 状态变化通知监听器
        }
        RpcRuntimeContext.cacheConsumerConfig(this);

        return (T) proxyObject;
    }

    /*
        /**
         * unRefer void.
         */
    @Override
    public synchronized void unRefer() {
        if (proxyIns == null) {
            return;
        }

        // Set to null is sufficient, since GPRC stub doesn't need to be closed.
        proxyIns = null;
    }

    // @Override
    // public Cluster getCluster() {
    //     throw new UnsupportedOperationException("Not supported");
    // }

    // @Override
    // public List<ProviderGroup> subscribe() {
    //     throw new UnsupportedOperationException("Not supported");
    // }

    // @Override
    // public boolean isSubscribed() {
    //     return proxyIns != null;
    // }

    /**
     * 得到实现代理类
     *
     * @return 实现代理类 proxy ins
     */
    @Override
    public T getProxyIns() {
        return proxyIns;
    }

    // @Override
    // protected T buildProxy() throws Exception {
    //     try {
    //         ExtensionClass<Proxy> ext = ExtensionLoaderFactory.getExtensionLoader(Proxy.class)
    //             .getExtensionClass(consumerConfig.getProxy());

    //         // Proxy proxy = ext.getExtInstance();
    //         // return (T) proxy.getProxyForClass(ClassUtils.forName(consumerConfig.getInterfaceId()+"$GreeterBlockingStub"), proxyInvoker); 
    //         // return (T) proxy.getProxy(ClassUtils.forName(consumerConfig.getInterfaceId()+"$GreeterBlockingStub") , proxyInvoker);
    //         return (T) getProxyForClass1(
    //             ClassUtils.forName("io.grpc.examples.helloworld.GreeterGrpc$GreeterBlockingStub"), proxyInvoker);
    //     } catch (SofaRpcRuntimeException e) {
    //         throw e;
    //     } catch (Throwable e) {
    //         throw new SofaRpcRuntimeException(e.getMessage(), e);
    //     }
    // }

    // static public ManagedChannel getManagedChannel() {
    //     String host = "127.0.0.1";
    //     int port = 50052;
    //     ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    //     return channel;
    // }

    // static public io.grpc.stub.AbstractStub getBlockingStub() {
    //     io.grpc.stub.AbstractStub stub = null;
    //     try {
    //         Method newBlockingStubMethod = Class.forName("io.grpc.examples.helloworld.GreeterGrpc")
    //             .getDeclaredMethod("newBlockingStub", Channel.class);
    //         newBlockingStubMethod.setAccessible(true);
    //         stub = (io.grpc.stub.AbstractStub) newBlockingStubMethod.invoke(null, getManagedChannel());

    //     } catch (ClassNotFoundException e) {
    //         LOGGER.error("ClassNotFoundException");

    //     } catch (IllegalAccessException e) {
    //         LOGGER.error("IllegalAccessException");

    //     } catch (NoSuchMethodException e) {
    //         LOGGER.error("NoSuchMethodException");

    //     } catch (InvocationTargetException e) {
    //         LOGGER.error("InvocationTargetException");

    //     } catch (IllegalArgumentException e) {
    //         LOGGER.error("IllegalArgumentException");
    //     }
    //     return stub;

    // }

}
