package com.playops.api.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;

public class ResizableThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private ResizableCapacityLinkedBlockingQueue<Runnable> workQueue;
    private int configuredQueueCapacity;

    @Override
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        configuredQueueCapacity = Math.max(0, queueCapacity);
        workQueue = new ResizableCapacityLinkedBlockingQueue<>(configuredQueueCapacity);
        return workQueue;
    }

    public int configuredQueueCapacity() {
        if (workQueue != null) {
            return workQueue.configuredCapacity();
        }
        return configuredQueueCapacity;
    }

    public void setConfiguredQueueCapacity(int queueCapacity) {
        configuredQueueCapacity = Math.max(0, queueCapacity);
        if (workQueue != null) {
            workQueue.setConfiguredCapacity(configuredQueueCapacity);
        }
    }
}
