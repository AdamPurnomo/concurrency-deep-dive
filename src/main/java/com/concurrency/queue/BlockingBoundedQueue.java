package com.concurrency.queue;

public class BlockingBoundedQueue<T> {
    private final int capacity;
    private final T[] queue;
    private int head;
    private int tail;
    private int size;


    public BlockingBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity cannot be null");
        }
        this.capacity = capacity;
        this.queue = (T[]) new Object[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public void put(T object) throws InterruptedException {
        if (object == null) {
            throw new IllegalArgumentException("Inserted object cannot be null");
        }

        synchronized (this) {
            while (size == capacity) {
                this.wait(); // release the lock and acquire upon wakes up
            }

            queue[tail] = object;
            tail = (tail + 1) % capacity;
            size += 1;
            this.notifyAll(); // release lock that it is available to take
        }

    }

    public T take() throws InterruptedException {
        T object;
        synchronized (this) {
            while (size == 0) {
                this.wait();
            }
            object = queue[head];
            queue[head] = null;
            head = (head + 1) % capacity;
            size -= 1;
            this.notifyAll();
        }
        return object;

    }
}
