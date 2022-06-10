package com.alipay.sofa.rpc.common.threadpool;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.Assert;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

public class MemorySafeLinkedBlockingQueueTest {

    @Test
    public void test() throws Exception {
        ByteBuddyAgent.install();
        final Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
        final long objectSize = instrumentation.getObjectSize((Runnable) () -> {
        });
        int maxFreeMemory = (int) MemoryLimitCalculator.maxAvailable();
        MemorySafeLinkedBlockingQueue<Runnable> queue = new MemorySafeLinkedBlockingQueue<>(maxFreeMemory);

        // all memory is reserved for JVM, so it will fail here
        Assert.assertEquals(queue.offer(() -> {
        }), false);

        // maxFreeMemory-objectSize Byte memory is reserved for the JVM, so this will succeed
        queue.setMaxFreeMemory((int) (MemoryLimitCalculator.maxAvailable() - objectSize));
        Assert.assertEquals(queue.offer(() -> {
        }), true);
    }

}