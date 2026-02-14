package com.concurrency.queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingBoundedQueue<T> {
    private final int capacity;
    private final T[] queue;
    private int head;
    private int tail;
    private final AtomicInteger size;

    private final ReentrantLock writeLock;
    private final ReentrantLock readLock;
    private final Condition notFull;
    private final Condition notEmpty;

    public BlockingBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.queue = (T[]) new Object[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = new AtomicInteger(0);
        this.writeLock = new ReentrantLock();
        this.readLock = new ReentrantLock();
        this.notFull = writeLock.newCondition();
        this.notEmpty = readLock.newCondition();
    }

    public void put(T object) throws InterruptedException {
        if (object == null) {
            throw new IllegalArgumentException("Inserted object cannot be null");
        }

        writeLock.lockInterruptibly();
        int prevSize;
        try {
            while (size.get() == capacity) {
                notFull.await();
            }

            queue[tail] = object;
            tail = (tail + 1) % capacity;
            prevSize = size.getAndIncrement();

            // Just an early optimization to wake other writers to wake
            // so that they don't have to wait for consumer to wake them up.
            if (size.get() < capacity) {
                notFull.signal();
            }
        } finally {
            writeLock.unlock();
        }

        // Only signal in transition (when previously empty and now not empty)
        if (prevSize == 0) {
            signalNotEmpty();
        }
    }

    public T take() throws InterruptedException {
        readLock.lockInterruptibly();
        T object;
        int prevSize;
        try {
            while (size.get() == 0) {
                notEmpty.await();
            }

            object = queue[head];
            queue[head] = null;
            head = (head + 1) % capacity;
            prevSize = size.getAndDecrement();

            // Just an early optimization to wake other readers to wake
            // so that they don't have to wait for producer to wake them up.
            if (size.get() > 0) {
                notEmpty.signal();
            }
        } finally {
            readLock.unlock();
        }

        // Only signal in transition (when previously full and now not full)
        if (prevSize == capacity) {
            signalNotFull();
        }
        return object;
    }

    private void signalNotEmpty() {
        readLock.lock();
        try {
            notEmpty.signal();
        } finally {
            readLock.unlock();
        }
    }

    private void signalNotFull() {
        writeLock.lock();
        try {
            notFull.signal();
        } finally {
            writeLock.unlock();
        }
    }
}
