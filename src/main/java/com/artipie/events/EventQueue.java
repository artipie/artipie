/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.events;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Events queue with {@link ConcurrentLinkedQueue} under the hood.
 * Instance of this class can be passed where necessary (to the adapters for example)
 * to add data for processing into the queue.
 * @param <T> Queue item parameter type.
 * @since 0.31
 */
public final class EventQueue<T> {

    /**
     * Queue.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final Queue<T> queue;

    /**
     * Ctor.
     */
    public EventQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Add item to queue.
     * @param item Element to add
     */
    public void put(final T item) {
        this.queue.add(item);
    }

    /**
     * Queue, not public intentionally, the queue should be accessible only from this package.
     * @return The queue.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    Queue<T> queue() {
        return this.queue;
    }
}
