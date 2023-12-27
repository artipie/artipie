/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Content;
import io.reactivex.Single;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ContentAs}.
 * @since 0.33
 */
class ContentAsTest {

    @Test
    void transformsToString() throws Exception {
        final String str = "abc012";
        MatcherAssert.assertThat(
            ContentAs.STRING.apply(Single.just(new Content.From(str.getBytes()))).toFuture().get(),
            new IsEqual<>(str)
        );
    }

    @Test
    void transformsToBytes() throws Exception {
        final byte[] bytes = "876hgf".getBytes();
        MatcherAssert.assertThat(
            ContentAs.BYTES.apply(Single.just(new Content.From(bytes))).toFuture().get(),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void transformsToLong() throws Exception {
        final long number = 12_087L;
        MatcherAssert.assertThat(
            ContentAs.LONG.apply(
                Single.just(new Content.From(String.valueOf(number).getBytes()))
            ).toFuture().get(),
            new IsEqual<>(number)
        );
    }

}
