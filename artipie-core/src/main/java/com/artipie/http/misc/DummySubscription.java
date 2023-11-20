/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import org.reactivestreams.Subscription;

/**
 * Dummy subscription that do nothing.
 * It's a requirement of reactive-streams specification to
 * call {@code onSubscribe} on subscriber before any other call.
 */
public enum DummySubscription implements Subscription {
    /**
     * Dummy value.
     */
    VALUE;

    @Override
    public void request(final long amount) {
        // does nothing
    }

    @Override
    public void cancel() {
        // does nothing
    }
}
