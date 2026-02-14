package com.concurrency.queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingBoundedQueue<T> {
    private final int capacity;
    private final T[] queue;
    private int head;
    private int tail;
    private int size;

    private final ReentrantLock lock;
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
        this.size = 0;
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
    }

    public void put(T object) throws InterruptedException {
        if (object == null) {
            throw new IllegalArgumentException("Inserted object cannot be null");
        }

        lock.lockInterruptibly();
        try {
            while (size == capacity) {
                notFull.await();
            }

            queue[tail] = object;
            tail = (tail + 1) % capacity;
            size += 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size == 0) {
                notEmpty.await();
            }

            T object = queue[head];
            queue[head] = null;
            head = (head + 1) % capacity;
            size -= 1;
            notFull.signal();
            return object;
        } finally {
            lock.unlock();
        }
    }
}
