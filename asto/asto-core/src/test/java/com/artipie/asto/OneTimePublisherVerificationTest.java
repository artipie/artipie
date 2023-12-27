/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Reactive streams-tck verification suit for {@link OneTimePublisher}.
 * @since 0.23
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class OneTimePublisherVerificationTest extends PublisherVerification<Integer> {

    /**
     * Ctor.
     */
    public OneTimePublisherVerificationTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Integer> createPublisher(final long elements) {
        return Flowable.empty();
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        final OneTimePublisher<Integer> publisher = new OneTimePublisher<>(Flowable.fromArray(1));
        Flowable.fromPublisher(publisher).toList().blockingGet();
        return publisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 0;
    }
}
