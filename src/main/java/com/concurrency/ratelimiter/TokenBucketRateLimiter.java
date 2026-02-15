    package com.concurrency.ratelimiter;

    import java.util.concurrent.atomic.AtomicReference;
    import java.util.concurrent.locks.LockSupport;

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
            if (permits <= 0 || permits > (int) capacity) {
                throw new IllegalArgumentException("Permits must be between 1 and " + (int) capacity);
            }

            while (true) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                final long now = System.nanoTime();
                // refill and take atomically
                State prev = state.getAndUpdate(s -> refillAndTake(s, now, permits));

                // We didn't know if the take (or rather the consumption was successful above).
                // The way we check it is a bit hacky. Since we know the previous version (before refilling),
                // we will try to replay the computation using the 'now' version passed to refill above.
                // We then check if the refilled bucket has more than requested permits. If it does, the above operation
                // was successful. This method is an indirect way to check if the above update operation was successful.
                // If it was successful, we break from the loop.
                State prevRefilled = refillAt(prev, now);
                if (prevRefilled.remainingToken >= permits) break;

                // Not enough: compute deficit based on the SAME refilled view
                double deficit = permits - prevRefilled.remainingToken;
                if (deficit <= 0) continue;

                // There are cases where Math.ceil give 0 even though it should be 1. Hence, clamping the minimum to be 1
                long sleepNs = Math.max(1L, (long) Math.ceil(deficit / tokenRefillRate));
                LockSupport.parkNanos(sleepNs);
            }
        }

        private State refillAndTake(State s, long now, int permits) {
            State r = refillAt(s, now);
            if (r.remainingToken >= permits) {
                return new State(r.remainingToken - permits, r.lastRefillTimeNs);
            }
            return r;
        }

        private State refillAt(State prev, long now) {
            long delta = now - prev.lastRefillTimeNs;
            if (delta <= 0) return prev;

            // Optional: avoid churn when already full
            if (prev.remainingToken >= capacity) {
                return new State(capacity, now);
            }

            double tokens = prev.remainingToken + tokenRefillRate * delta; // tokens/ns
            return new State(Math.min(tokens, capacity), now);
        }

        record State(
                double remainingToken,
                long lastRefillTimeNs
        ) {}
    }
