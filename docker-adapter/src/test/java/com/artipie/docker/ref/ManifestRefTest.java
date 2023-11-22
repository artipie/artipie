/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.ref;

import com.artipie.docker.Digest;
import com.artipie.docker.Tag;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test case for {@link ManifestRef}.
 * @since 0.1
 */
public final class ManifestRefTest {

    @Test
    void resolvesDigestString() {
        MatcherAssert.assertThat(
            new ManifestRef.FromString("sha256:1234").link().string(),
            Matchers.equalTo("revisions/sha256/1234/link")
        );
    }

    @Test
    void resolvesTagString() {
        MatcherAssert.assertThat(
            new ManifestRef.FromString("1.0").link().string(),
            Matchers.equalTo("tags/1.0/current/link")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a:b:c",
        ".123"
    })
    void failsToResolveInvalid(final String string) {
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ManifestRef.FromString(string).link().string()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Unsupported reference"),
                    new StringContains(false, string)
                )
            )
        );
    }

    @Test
    void resolvesDigestLink() {
        MatcherAssert.assertThat(
            new ManifestRef.FromDigest(new Digest.Sha256("0000")).link().string(),
            Matchers.equalTo("revisions/sha256/0000/link")
        );
    }

    @Test
    void resolvesTagLink() {
        MatcherAssert.assertThat(
            new ManifestRef.FromTag(new Tag.Valid("latest")).link().string(),
            Matchers.equalTo("tags/latest/current/link")
        );
    }

    @Test
    void stringFromDigestRef() {
        MatcherAssert.assertThat(
            new ManifestRef.FromDigest(new Digest.Sha256("0123")).string(),
            Matchers.equalTo("sha256:0123")
        );
    }

    @Test
    void stringFromTagRef() {
        final String tag = "0.2";
        MatcherAssert.assertThat(
            new ManifestRef.FromTag(new Tag.Valid(tag)).string(),
            Matchers.equalTo(tag)
        );
    }

    @Test
    void stringFromStringRef() {
        final String value = "whatever";
        MatcherAssert.assertThat(
            new ManifestRef.FromString(value).string(),
            Matchers.equalTo(value)
        );
    }
}
