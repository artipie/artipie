/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.llorllale.cactoos.matchers.Matches;

/**
 * Tests for {@link RsHasBody}.
 *
 * @since 0.4
 */
final class RsHasBodyTest {

    @Test
    void shouldMatchEqualBody() {
        final Response response = connection -> connection.accept(
            RsStatus.OK,
            Headers.EMPTY,
            Flowable.fromArray(
                ByteBuffer.wrap("he".getBytes()),
                ByteBuffer.wrap("ll".getBytes()),
                ByteBuffer.wrap("o".getBytes())
            )
        );
        MatcherAssert.assertThat(
            "Matcher is expected to match response with equal body",
            new RsHasBody("hello".getBytes()).matches(response),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldNotMatchNotEqualBody() {
        final Response response = connection -> connection.accept(
            RsStatus.OK,
            Headers.EMPTY,
            Flowable.fromArray(ByteBuffer.wrap("1".getBytes()))
        );
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
        final Response response = new RsWithBody(
            Flowable.fromIterable(
                Stream.of(elements)
                    .map(String::getBytes)
                    .map(ByteBuffer::wrap)
                    .collect(Collectors.toList())
            )
        );
        new RsHasBody(data).matches(response);
        MatcherAssert.assertThat(
            new RsHasBody(data).matches(response),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldWorkWithContainsMatcherMismatches() {
        MatcherAssert.assertThat(
            new RsHasBody("XXX"),
            new IsNot<>(
                new Matches<>(
                    new RsWithBody(
                        "xxx", StandardCharsets.UTF_8
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"bytes", "more bytes"})
    void shouldWorkWithContainsMatcher(final String content) {
        MatcherAssert.assertThat(
            new RsHasBody(
                Matchers.equalToIgnoringCase(content),
                StandardCharsets.UTF_8
            ),
            Matchers.<Matcher<Response>>allOf(
                new Matches<>(
                    new RsWithBody(
                        content,
                        StandardCharsets.UTF_8
                    )
                ),
                new Matches<>(
                    new RsWithBody(
                        content.toUpperCase(Locale.ROOT),
                        StandardCharsets.UTF_8
                    )
                )
            )
        );
    }
}
