/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Test case for {@link Pipeline}.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class PipelineTest {

    @Test
    void doesNothingOnSubscribe() {
        final Pipeline<?> target = new Pipeline<>();
        final AtomicLong requests = new AtomicLong();
        final AtomicBoolean cancellation = new AtomicBoolean();
        target.onSubscribe(new TestSubscription(requests, cancellation));
        MatcherAssert.assertThat("Requested item", requests.get(), new IsEqual<>(0L));
        MatcherAssert.assertThat(
            "Cancelled on subscribe", cancellation.get(), new IsEqual<>(false)
        );
    }

    @Test
    void doesNothingOnConnect() {
        final Pipeline<?> target = new Pipeline<>();
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        target.connect(new TestSubscriber(subscription));
        MatcherAssert.assertThat(subscription.get(), new IsNull<>());
    }

    @Test
    void subscribeAndRequestOnSubscribeAndConnect() {
        final Pipeline<?> target = new Pipeline<>();
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        final AtomicBoolean cancellation = new AtomicBoolean();
        final AtomicLong requests = new AtomicLong();
        target.onSubscribe(new TestSubscription(requests, cancellation));
        target.connect(new TestSubscriber(subscription));
        MatcherAssert.assertThat("Not requested one item", requests.get(), new IsEqual<>(1L));
        MatcherAssert.assertThat("Cancelled on request", cancellation.get(), new IsEqual<>(false));
        MatcherAssert.assertThat("Not subscribed", subscription.get(), new IsEqual<>(target));
    }

    /**
     * Test subscriber for pipeline.
     * @since 1.0
     */
    private static final class TestSubscriber implements Subscriber<Object> {

        /**
         * Test subscription.
         */
        private final AtomicReference<Subscription> subscription;

        /**
         * New test subscriber.
         * @param subscription Subscribtion
         */
        TestSubscriber(final AtomicReference<Subscription> subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onSubscribe(final Subscription sub) {
            if (!this.subscription.compareAndSet(null, sub)) {
                throw new IllegalStateException("Pipeline should not subscribe twice");
            }
        }

        @Override
        public void onNext(final Object next) {
            // do nothing
        }

        @Override
        public void onError(final Throwable err) {
            // do nothing
        }

        @Override
        public void onComplete() {
            // do nothing
        }
    }

    /**
     * Test subscription.
     * @since 1.0
     */
    private final class TestSubscription implements Subscription {

        /**
         * Amount of requested items.
         */
        private final AtomicLong requested;

        /**
         * Cancellation flag.
         */
        private final AtomicBoolean cancellation;

        /**
         * New subscription.
         * @param requested Requested counter
         * @param cancellation Cancelled counter
         */
        TestSubscription(final AtomicLong requested, final AtomicBoolean cancellation) {
            this.requested = requested;
            this.cancellation = cancellation;
        }

        // @checkstyle ReturnCountCheck (10 lines)
        @Override
        public void request(final long add) {
            this.requested.updateAndGet(
                old -> {
                    if (old + add < 0) {
                        return Long.MAX_VALUE;
                    }
                    return old + add;
                }
            );
        }

        @Override
        public void cancel() {
            this.cancellation.set(true);
        }
    }
}
