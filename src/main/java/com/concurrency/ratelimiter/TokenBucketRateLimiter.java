package com.concurrency.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketRateLimiter {
    private final int capacity;
    private final double tokenRefillRate;
    private AtomicInteger remainingToken;
    private AtomicLong lastRefillTimeNs;

    public TokenBucketRateLimiter(int capacity, double tokenRefillRate) {
        this.capacity = capacity;
        this.remainingToken = new AtomicInteger(capacity);
        this.lastRefillTimeNs = new AtomicLong(System.nanoTime());
        this.tokenRefillRate = tokenRefillRate;
    }

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquire(int permits) throws InterruptedException {

    }
}
