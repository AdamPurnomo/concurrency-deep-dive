package com.concurrency;

import com.concurrency.ratelimiter.RateLimiterSimulator;


public class Main {


    public static void main(String[] args) {
        final var simulator = new RateLimiterSimulator();
        simulator.execute();
    }
}
