/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.asto.Content;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for {@link RsHasBody}.
 */
final class RsHasBodyTest {

    @Test
    void shouldMatchEqualBody() {
        final Response response = ResponseBuilder.ok()
            .body(new Content.From(
                Flowable.fromArray(
                    ByteBuffer.wrap("he".getBytes()),
                    ByteBuffer.wrap("ll".getBytes()),
                    ByteBuffer.wrap("o".getBytes())
                )
            ))
            .build();
        MatcherAssert.assertThat(
            "Matcher is expected to match response with equal body",
            new RsHasBody("hello".getBytes()).matches(response),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldNotMatchNotEqualBody() {
        final Response response = ResponseBuilder.ok()
            .body(new Content.From(Flowable.fromArray(ByteBuffer.wrap("1".getBytes()))))
            .build();
        MatcherAssert.assertThat(
            "Matcher is expected not to match response with not equal body",
            new RsHasBody("2".getBytes()).matches(response),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"data", "chunk1,chunk2"})
    void shouldMatchResponseTwice(final String chunks) {
        final String[] elements = chunks.split(",");
        final byte[] data = String.join("", elements).getBytes();
        final Response response = ResponseBuilder.ok().body(
            Flowable.fromIterable(
                Stream.of(elements)
                    .map(String::getBytes)
                    .map(ByteBuffer::wrap)
                    .collect(Collectors.toList())
            )
        ).build();
        new RsHasBody(data).matches(response);
        Assertions.assertTrue(new RsHasBody(data).matches(response));
    }
}
