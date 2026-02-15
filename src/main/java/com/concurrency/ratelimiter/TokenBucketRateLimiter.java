package com.concurrency.ratelimiter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.UnaryOperator;

public class TokenBucketRateLimiter {
    private final long capacity;
    private final double tokenRefillRate;
    private AtomicReference<State> state;

    public TokenBucketRateLimiter(long capacity, double tokenRefillRate) {
        if (capacity <= 0 || tokenRefillRate <= 0) {
            throw new IllegalArgumentException("Capacity and token refill rate must be positive");
        }
        this.capacity = capacity;
        this.state = new AtomicReference<>(new State(capacity, System.nanoTime()));
        this.tokenRefillRate = tokenRefillRate;
    }

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits <= 0 || permits > capacity) {
            throw new IllegalArgumentException(String.format("Permits must be between 0 and %d", capacity));
        }

        final UnaryOperator<State> takeIfSufficient = (prevState) -> {
            if (prevState.remainingToken >= permits) {
                return new State(
                        prevState.remainingToken - permits,
                        prevState.lastRefillTimeNs
                );
            }
            return prevState;
        };

        state.getAndUpdate(this::refillToken);
        while (state.getAndUpdate(takeIfSufficient).remainingToken < permits) {
            final var deltaToken = permits - state.get().remainingToken;
            final var sleepTime = (long) (deltaToken / tokenRefillRate);
            LockSupport.parkNanos(sleepTime);
            state.updateAndGet(this::refillToken);
        }
    }

    private State refillToken(final State prevState) {
        final long time = System.nanoTime();
        final var delta = time - prevState.lastRefillTimeNs;
        final var token = (long) Math.floor(prevState.remainingToken + tokenRefillRate * delta);
        return new State(Math.min(token, capacity), time);
    }

    record State(
            long remainingToken,
            long lastRefillTimeNs
    ) {}
}
