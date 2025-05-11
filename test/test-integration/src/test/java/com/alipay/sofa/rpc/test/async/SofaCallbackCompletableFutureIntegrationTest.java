package com.alipay.sofa.rpc.test.async;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.SofaCallbackCompletableFuture;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SofaCallbackCompletableFutureIntegrationTest extends ActivelyDestroyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncChainTest.class);

    @Test
    public void testAsyncHelloServiceWithSofaCallbackCompletableFuture() throws Exception {
        int cPort = 22300;
        int bPort = 22301;

        // 启动C服务
        ProviderConfig<HelloService> cProvider = new ProviderConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl(100))
                .setServer(new ServerConfig().setPort(cPort).setDaemon(false));
        cProvider.export();

        // 启动B服务，依赖C
        ConsumerConfig<HelloService> b2cConsumer = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
                .setTimeout(2000)
                .setDirectUrl("bolt://127.0.0.1:" + cPort);
        HelloService helloService = b2cConsumer.refer();

        ProviderConfig<AsyncHelloService> bProvider = new ProviderConfig<AsyncHelloService>()
                .setInterfaceId(AsyncHelloService.class.getName())
                .setRef(new AsyncHelloServiceImpl(helloService))
                .setServer(new ServerConfig().setPort(bPort).setDaemon(false));
        bProvider.export();

        // 启动A客户端
        ConsumerConfig<AsyncHelloService> a2bConsumer = new ConsumerConfig<AsyncHelloService>()
                .setInterfaceId(AsyncHelloService.class.getName())
                .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
                .setTimeout(2000)
                .setDirectUrl("bolt://127.0.0.1:" + bPort);
        AsyncHelloService asyncHelloService = a2bConsumer.refer();

        try {
            // 1. 正常链式异步
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future1 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("foo", 18);
            future1.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            String result = future1
                    .thenApply(res -> res + " ok")
                    .get(2000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(result.contains("hello"));

            // 2. 业务异常
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future2 = SofaCallbackCompletableFuture.create();
            asyncHelloService.appException("bar");
            future2.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get sofa exception: ", ex);
                }
            });
            try {
                future2.get(2000, TimeUnit.MILLISECONDS);
                Assert.fail("Should throw ExecutionException");
            } catch (ExecutionException e) {
                Assert.assertTrue(e.getCause() instanceof RuntimeException);
                Assert.assertEquals("1234", e.getCause().getMessage());
            }

            // 3. RPC异常
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future3 = SofaCallbackCompletableFuture.create();
            asyncHelloService.rpcException("baz");
            future3.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get sofa exception: ", ex);
                }
            });
            try {
                future3.get(2000, TimeUnit.MILLISECONDS);
                Assert.fail("Should throw ExecutionException");
            } catch (ExecutionException e) {
                Assert.assertTrue(e.getCause() instanceof SofaRpcException);
                Assert.assertTrue(e.getCause().getMessage().contains("bbb"));
            }

            // 4. 超时异常
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future4 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("timeout", 0);
            future4.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            try {
                future4.get(100, TimeUnit.MILLISECONDS);
                Assert.fail("Should throw TimeoutException");
            } catch (TimeoutException e) {
                // 预期超时
            } catch (ExecutionException e) {
                Assert.assertTrue(e.getCause() instanceof SofaRpcException);
            }

            // 5. 并发异步请求
            int count = 10;
            List<SofaCallbackCompletableFuture<String>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                RpcInvokeContext.removeContext();
                SofaCallbackCompletableFuture<String> f = SofaCallbackCompletableFuture.create();
                asyncHelloService.sayHello("foo" + i, i);
                f.whenComplete((res, ex) -> {
                    if (ex == null) {
                        LOGGER.info("A get result: " + res);
                    } else {
                        LOGGER.error("A get exception: ", ex);
                    }
                });
                futures.add(f);
            }
            for (int i = 0; i < count; i++) {
                String r = futures.get(i).get(2000, TimeUnit.MILLISECONDS);
                Assert.assertTrue(r.contains("foo" + i));
            }

            // 6. 回调链路抛出异常
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future5 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("foo", 1);
            future5.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            try {
                future5.thenApply(res -> { throw new RuntimeException("callback error"); }).get();
                Assert.fail("Should throw ExecutionException");
            } catch (ExecutionException e) {
                System.out.println("Exception: " + e.getMessage());
                Assert.assertTrue(e.getCause() instanceof RuntimeException);
                Assert.assertEquals("callback error", e.getCause().getMessage());
            }

            // 7. 取消 future
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> future7 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("foo", 1);
            future7.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            boolean cancelled = future7.cancel(true);
            Assert.assertTrue(cancelled);
            Assert.assertTrue(future7.isCancelled());

            // 8. thenCombine 链式组合
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> f1 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("foo", 1);
            f1.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            RpcInvokeContext.removeContext();
            SofaCallbackCompletableFuture<String> f2 = SofaCallbackCompletableFuture.create();
            asyncHelloService.sayHello("bar", 2);
            f2.whenComplete((res, ex) -> {
                if (ex == null) {
                    LOGGER.info("A get result: " + res);
                } else {
                    LOGGER.error("A get exception: ", ex);
                }
            });
            String combined = f1.thenCombine(f2, (r1, r2) -> r1 + "+" + r2).get();
            Assert.assertTrue(combined.contains("foo") && combined.contains("bar"));

        } finally {
            a2bConsumer.unRefer();
            bProvider.unExport();
            b2cConsumer.unRefer();
            cProvider.unExport();
        }
    }
}