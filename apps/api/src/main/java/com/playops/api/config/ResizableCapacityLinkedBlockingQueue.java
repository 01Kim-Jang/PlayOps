package com.playops.api.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ResizableCapacityLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private final AtomicInteger capacity;

    public ResizableCapacityLinkedBlockingQueue(int capacity) {
        super(Integer.MAX_VALUE);
        this.capacity = new AtomicInteger(Math.max(0, capacity));
    }

    @Override
    public boolean offer(E e) {
        if (size() >= capacity.get()) {
            return false;
        }
        return super.offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (size() >= capacity.get()) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        return super.offer(e);
    }

    @Override
    public int remainingCapacity() {
        return Math.max(0, capacity.get() - size());
    }

    public int configuredCapacity() {
        return capacity.get();
    }

    public void setConfiguredCapacity(int capacity) {
        this.capacity.set(Math.max(0, capacity));
    }
}
