/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http.publish;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import com.artipie.http.Headers;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Multipart}.
 *
 * @since 0.1
 */
class MultipartTest {

    @Test
    void shouldReadFirstPart() {
        final Multipart multipart = new Multipart(
            new Headers.From("Content-Type", "multipart/form-data; boundary=\"simple boundary\""),
            Flowable.just(
                ByteBuffer.wrap(
                    String.join(
                        "",
                        "--simple boundary\r\n",
                        "Some-Header: info\r\n",
                        "\r\n",
                        "data\r\n",
                        "--simple boundary--"
                    ).getBytes()
                )
            )
        );
        MatcherAssert.assertThat(
            new Remaining(new Concatenation(multipart.first()).single().blockingGet()).bytes(),
            new IsEqual<>("data".getBytes())
        );
    }

    @Test
    void shouldFailIfNoContentTypeHeader() {
        final Multipart multipart = new Multipart(Collections.emptySet(), Flowable.empty());
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> Flowable.fromPublisher(multipart.first()).blockingFirst()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new IsEqual<>("Cannot find header \"Content-Type\"")
        );
    }

    @Test
    void shouldFailIfNoParts() {
        final Multipart multipart = new Multipart(
            new Headers.From("content-type", "multipart/form-data; boundary=123"),
            Flowable.just(ByteBuffer.wrap("--123--".getBytes()))
        );
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> Flowable.fromPublisher(multipart.first()).blockingFirst()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new IsEqual<>("Body has no parts")
        );
    }
}
