/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.fake.FakeManifests;
import com.artipie.docker.fake.FullTagsManifests;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Arrays;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Tests for {@link MultiReadManifests}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MultiReadManifestsTest {

    @ParameterizedTest
    @CsvSource({
        "empty,empty,",
        "empty,full,two",
        "full,empty,one",
        "faulty,full,two",
        "full,faulty,one",
        "faulty,empty,",
        "empty,faulty,",
        "full,full,one"
    })
    void shouldReturnExpectedValue(
        final String origin,
        final String cache,
        final String expected
    ) {
        final MultiReadManifests manifests = new MultiReadManifests(
            new RepoName.Simple("test"),
            Arrays.asList(
                new FakeManifests(origin, "one"),
                new FakeManifests(cache, "two")
            )
        );
        MatcherAssert.assertThat(
            manifests.get(new ManifestRef.FromString("ref"))
                .toCompletableFuture().join()
                .map(Manifest::digest)
                .map(Digest::hex),
            new IsEqual<>(Optional.ofNullable(expected))
        );
    }

    @Test
    void loadsTagsFromManifests() {
        final int limit = 3;
        final String name = "tags-test";
        MatcherAssert.assertThat(
            new MultiReadManifests(
                new RepoName.Simple(name),
                Arrays.asList(
                    new FullTagsManifests(
                        () -> new Content.From("{\"tags\":[\"one\",\"three\",\"four\"]}".getBytes())
                    ),
                    new FullTagsManifests(
                        () -> new Content.From("{\"tags\":[\"one\",\"two\"]}".getBytes())
                    )
                )
            ).tags(Optional.of(new Tag.Valid("four")), limit).thenCompose(
                tags -> new PublisherAs(tags.json()).asciiString()
            ).toCompletableFuture().join(),
            new StringIsJson.Object(
                Matchers.allOf(
                    new JsonHas("name", new JsonValueIs(name)),
                    new JsonHas(
                        "tags",
                        new JsonContains(
                            new JsonValueIs("one"), new JsonValueIs("three"), new JsonValueIs("two")
                        )
                    )
                )
            )
        );
    }
}
