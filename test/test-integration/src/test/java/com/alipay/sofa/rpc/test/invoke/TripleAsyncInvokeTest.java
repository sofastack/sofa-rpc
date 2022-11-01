package com.alipay.sofa.rpc.test.invoke;

import com.alipay.sofa.rpc.api.future.SofaResponseFuture;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author gujin
 * Created on 2022/11/1 3:01 下午
 */
public class TripleAsyncInvokeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleAsyncInvokeTest.class);

    private static HelloService future;
    private static HelloService callback;

    @BeforeClass
    public static void start() {
        ServerConfig serverConfig2 = new ServerConfig()
                .setPort(22223)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDaemon(false);

        // 服务端
        ProviderConfig<HelloService> CProvider = new ProviderConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl(1000))
                .setServer(serverConfig2);
        CProvider.export();

        // 客户端
        ConsumerConfig<HelloService> BConsumer = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
                .setTimeout(5000)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("127.0.0.1:22223");
        future = BConsumer.refer();

        ConsumerConfig<HelloService> BBConsumer = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
                .setTimeout(5000)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("127.0.0.1:22223");
        callback = BBConsumer.refer();
    }

    @Test
    public void testFuture() {
        boolean error = false;
        try {
            String ret = future.sayHello("xxx", 22);
            Assert.assertNull(ret); // 第一次返回null
            Future future = SofaResponseFuture.getFuture();
            ret = (String) future.get();
            Assert.assertNotNull(ret);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
    }

    @Test
    public void testCallback() {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] ret = { null };

        RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                LOGGER.info("B get result: {}", appResponse);
                ret[0] = (String) appResponse;
                latch.countDown();
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                LOGGER.info("B get app exception: {}", throwable);
                latch.countDown();
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName,
                                        RequestBase request) {
                LOGGER.info("B get sofa exception: {}", sofaException);
                latch.countDown();
            }
        });

        String ret0 = callback.sayHello("xxx", 22);
        Assert.assertNull(ret0); // 第一次返回null

        try {
            latch.await(60000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }

        Assert.assertNotNull(ret[0]);
    }

}
