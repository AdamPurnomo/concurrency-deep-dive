package com.concurrency.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueSimulator {
    private static final int QUEUE_CAPACITY = 5;
    private static final int PRODUCER_COUNT = 5;
    private static final int CONSUMER_COUNT = 5;
    private static final int ITEMS_PER_PRODUCER = 10;

    public void execute() {
        BlockingBoundedQueue<Integer> queue = new BlockingBoundedQueue<>(QUEUE_CAPACITY);
        AtomicInteger sequence = new AtomicInteger(1);
        List<Thread> workers = new ArrayList<>();

        for (int i = 1; i <= PRODUCER_COUNT; i++) {
            final int workerId = i;
            Thread producer =
                    new Thread(
                            () -> {
                                try {
                                    for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                                        int value = sequence.getAndIncrement();
                                        queue.put(value);
                                        System.out.println("Producer-" + workerId + " put " + value);
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "producer-" + workerId);
            workers.add(producer);
        }

        int itemsPerConsumer = (PRODUCER_COUNT * ITEMS_PER_PRODUCER) / CONSUMER_COUNT;
        for (int i = 1; i <= CONSUMER_COUNT; i++) {
            final int workerId = i;
            Thread consumer =
                    new Thread(
                            () -> {
                                try {
                                    for (int j = 0; j < itemsPerConsumer; j++) {
                                        Integer value = queue.take();
                                        System.out.println("Consumer-" + workerId + " took " + value);
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "consumer-" + workerId);
            workers.add(consumer);
        }

        // start all workers
        for (Thread worker : workers) {
            worker.start();
        }

        // wait until all workers to finish
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("All workers finished.");
    }
}
