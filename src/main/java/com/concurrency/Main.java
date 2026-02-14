package com.concurrency;

import com.concurrency.queue.QueueSimulator;


public class Main {


    public static void main(String[] args) {
        final var simulator = new QueueSimulator();
        simulator.execute();
    }
}
