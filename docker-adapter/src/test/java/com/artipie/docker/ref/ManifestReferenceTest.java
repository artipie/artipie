/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.ref;

import com.artipie.docker.Digest;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.error.InvalidTagNameException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

/**
 * Test case for {@link ManifestReference}.
 */
public final class ManifestReferenceTest {

    @Test
    void resolvesDigestString() {
        MatcherAssert.assertThat(
            ManifestReference.from("sha256:1234").link().string(),
            Matchers.equalTo("revisions/sha256/1234/link")
        );
    }

    @Test
    void resolvesTagString() {
        MatcherAssert.assertThat(
            ManifestReference.from("1.0").link().string(),
            Matchers.equalTo("tags/1.0/current/link")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a:b:c",
        ".123"
    })
    void failsToResolveInvalid(final String tag) {
        final Throwable throwable = Assertions.assertThrows(
            InvalidTagNameException.class,
            () -> ManifestReference.from(tag).link().string()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Invalid tag"),
                    new StringContains(false, tag)
                )
            )
        );
    }

    @Test
    void resolvesDigestLink() {
        MatcherAssert.assertThat(
            ManifestReference.from(new Digest.Sha256("0000")).link().string(),
            Matchers.equalTo("revisions/sha256/0000/link")
        );
    }

    @Test
    void resolvesTagLink() {
        MatcherAssert.assertThat(
            ManifestReference.fromTag("latest").link().string(),
            Matchers.equalTo("tags/latest/current/link")
        );
    }

    @Test
    void stringFromDigestRef() {
        MatcherAssert.assertThat(
            ManifestReference.from(new Digest.Sha256("0123")).reference(),
            Matchers.equalTo("sha256:0123")
        );
    }

    @Test
    void stringFromTagRef() {
        final String tag = "0.2";
        MatcherAssert.assertThat(
            ManifestReference.fromTag(tag).reference(),
            Matchers.equalTo(tag)
        );
    }

    @Test
    void stringFromStringRef() {
        final String value = "whatever";
        MatcherAssert.assertThat(
            ManifestReference.from(value).reference(),
            Matchers.equalTo(value)
        );
    }
}
