    package com.concurrency.ratelimiter;

    import java.util.concurrent.atomic.AtomicReference;
    import java.util.concurrent.locks.LockSupport;
    import java.util.function.UnaryOperator;

    public class TokenBucketRateLimiter {
        private final double capacity;
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
                throw new IllegalArgumentException(String.format("Permits must be between 1 and %d", capacity));
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
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                final var deficit = permits - state.get().remainingToken;
                // Turns out it has been refilled, retry again
                if (deficit <= 0) {continue;}

                final var sleepTime = (long) Math.ceil(deficit / tokenRefillRate);
                LockSupport.parkNanos(sleepTime);
                state.updateAndGet(this::refillToken);
            }
        }

        private State refillToken(final State prevState) {
            final long time = System.nanoTime();
            final var delta = time - prevState.lastRefillTimeNs;

            // Just an additional guard in case System.nanoTime() isn't consistent (monotonically increasing
            if (delta <= 0) {
                return prevState;
            }
            final var token = prevState.remainingToken + tokenRefillRate * delta;
            return new State(Math.min(token, capacity), time);
        }

        record State(
                double remainingToken,
                long lastRefillTimeNs
        ) {}
    }
