/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.Content;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Whitebox style tests for verifying {@link StorageValuePipeline.ContentAsInputStream}.
 *
 * @since 1.12
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class ContentAsInputStreamWhiteboxVerificationTest
    extends SubscriberWhiteboxVerification<ByteBuffer> {

    /**
     * Ctor.
     */
    ContentAsInputStreamWhiteboxVerificationTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(
        final WhiteboxSubscriberProbe<ByteBuffer> probe
    ) {
        return new SubscriberWithProbe(
            new StorageValuePipeline.ContentAsInputStream(
                new Content.From("data".getBytes())
            ),
            probe
        );
    }

    @Override
    public ByteBuffer createElement(final int element) {
        final byte[] arr = new byte[24];
        Arrays.fill(arr, (byte) element);
        return ByteBuffer.wrap(arr);
    }

    /**
     * Subscriber with probe.
     *
     * @since 1.12
     */
    private static class SubscriberWithProbe implements Subscriber<ByteBuffer> {

        /**
         * Target subscriber.
         */
        private final StorageValuePipeline.ContentAsInputStream target;

        /**
         * Test probe.
         */
        private final WhiteboxSubscriberProbe<ByteBuffer> probe;

        /**
         * Ctor.
         *
         * @param target Subscriber
         * @param probe For test
         */
        SubscriberWithProbe(
            final StorageValuePipeline.ContentAsInputStream target,
            final WhiteboxSubscriberProbe<ByteBuffer> probe
        ) {
            this.target = target;
            this.probe = probe;
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            this.target.onSubscribe(subscription);
            this.probe.registerOnSubscribe(new ProbePuppet(subscription));
        }

        @Override
        public void onNext(final ByteBuffer next) {
            this.target.onNext(next);
            this.probe.registerOnNext(next);
        }

        @Override
        public void onError(final Throwable err) {
            this.target.onError(err);
            this.probe.registerOnError(err);
        }

        @Override
        public void onComplete() {
            this.target.onComplete();
            this.probe.registerOnComplete();
        }
    }

    /**
     * Puppet for subscriber probe.
     *
     * @since 1.12
     */
    private static class ProbePuppet implements SubscriberPuppet {

        /**
         * Actual subscription.
         */
        private final Subscription subscription;

        /**
         * New puppet.
         *
         * @param subscription Of subscriber
         */
        ProbePuppet(final Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void triggerRequest(final long elements) {
            this.subscription.request(elements);
        }

        @Override
        public void signalCancel() {
            this.subscription.cancel();
        }
    }
}
