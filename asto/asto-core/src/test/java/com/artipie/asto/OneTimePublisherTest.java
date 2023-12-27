/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link OneTimePublisher}.
 * @since 0.23
 */
public final class OneTimePublisherTest {

    @Test
    public void secondAttemptLeadToFail() {
        final int one = 1;
        final Flowable<Integer> pub = Flowable.fromPublisher(
            new OneTimePublisher<>(Flowable.fromArray(one))
        );
        final Integer last = pub.lastOrError().blockingGet();
        MatcherAssert.assertThat(last, new IsEqual<>(one));
        Assertions.assertThrows(
            ArtipieIOException.class,
            () -> pub.firstOrError().blockingGet()
        );
    }
}
