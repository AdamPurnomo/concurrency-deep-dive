package com.concurrency.ratelimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiterSimulator {
    private static final int CAPACITY = 10;
    private static final double REFILL_RATE = 2.0 / 1_000_000_000; // 2 tokens per second (tokens per nanosecond)
    private static final int THREAD_COUNT = 5;
    private static final int REQUESTS_PER_THREAD = 10;

    public void execute() {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(CAPACITY, REFILL_RATE);
        AtomicInteger totalPermitsAcquired = new AtomicInteger(0);
        List<Thread> workers = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Create worker threads
        for (int i = 1; i <= THREAD_COUNT; i++) {
            final int workerId = i;
            Thread worker = new Thread(
                () -> {
                    Random random = new Random();
                    try {
                        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                            // Request random permits (1 to CAPACITY-1)
                            int permits = random.nextInt(CAPACITY - 1) + 1;

                            long beforeAcquire = System.currentTimeMillis();
                            rateLimiter.acquire(permits);
                            long afterAcquire = System.currentTimeMillis();

                            totalPermitsAcquired.addAndGet(permits);
                            long waitTime = afterAcquire - beforeAcquire;

                            System.out.println(
                                String.format("Thread-%d acquired %d permits (waited %dms)",
                                    workerId, permits, waitTime)
                            );
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Thread-" + workerId + " interrupted");
                    }
                },
                "worker-" + workerId
            );
            workers.add(worker);
        }

        // Start all workers
        for (Thread worker : workers) {
            worker.start();
        }

        // Wait for all workers to finish
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long elapsedTimeMs = endTime - startTime;
        double elapsedTimeSec = elapsedTimeMs / 1000.0;

        System.out.println("\n=== Simulation Complete ===");
        System.out.println("Total permits acquired: " + totalPermitsAcquired.get());
        System.out.println("Total time: " + elapsedTimeSec + " seconds");
        System.out.println("Throughput: " +
            String.format("%.2f", totalPermitsAcquired.get() / elapsedTimeSec) +
            " permits/second");
        System.out.println("Expected rate: " + (REFILL_RATE * 1_000_000_000) + " permits/second");
    }
}
