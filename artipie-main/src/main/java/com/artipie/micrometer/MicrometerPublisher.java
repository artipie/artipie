/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import io.micrometer.core.instrument.DistributionSummary;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Publisher decorator which counts number of inner chunks and number of bytes.
 * To get these amounts micrometer {@link DistributionSummary}
 * is used, as it always publish a count of events in addition to main measure.
 * <a href="https://micrometer.io/docs/concepts#_distribution_summaries">Docs</a>.
 * @since 0.28
 */
public final class MicrometerPublisher implements Content {

    /**
     * Origin publisher.
     */
    private final Content origin;

    /**
     * Micrometer distribution summary.
     */
    private final DistributionSummary summary;

    /**
     * Ctor.
     * @param origin Origin content
     * @param summary Micrometer distribution summary
     */
    public MicrometerPublisher(final Content origin, final DistributionSummary summary) {
        this.origin = origin;
        this.summary = summary;
    }

    /**
     * Ctor.
     * @param origin Origin publisher
     * @param summary Micrometer distribution summary
     */
    public MicrometerPublisher(final Publisher<ByteBuffer> origin,
        final DistributionSummary summary) {
        this(new Content.From(origin), summary);
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.origin.subscribe(new MicrometerSubscriber(subscriber, this.summary));
    }

    @Override
    public Optional<Long> size() {
        return this.origin.size();
    }

    /**
     * Micrometer subscriber.
     * @since 0.28
     */
    private static final class MicrometerSubscriber implements Subscriber<ByteBuffer> {

        /**
         * Origin subscriber.
         */
        private final Subscriber<? super ByteBuffer> origin;

        /**
         * Micrometer distribution summary.
         */
        private final DistributionSummary summary;

        /**
         * Wrap subscriber.
         * @param origin Origin subscriber
         * @param summary Micrometer distribution summary
         */
        MicrometerSubscriber(final Subscriber<? super ByteBuffer> origin,
            final DistributionSummary summary) {
            this.origin = origin;
            this.summary = summary;
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            this.origin.onSubscribe(subscription);
        }

        @Override
        public void onNext(final ByteBuffer buffer) {
            this.summary.record(buffer.remaining());
            this.origin.onNext(buffer);
        }

        @Override
        public void onError(final Throwable err) {
            this.origin.onError(err);
        }

        @Override
        public void onComplete() {
            this.origin.onComplete();
        }
    }
}
