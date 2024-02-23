/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Digest;
import com.artipie.docker.error.InvalidManifestException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonManifest}.
 *
 * @since 0.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class JsonManifestTest {

    @Test
    void shouldReadMediaType() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            "{\"mediaType\":\"something\"}".getBytes()
        );
        Assertions.assertEquals(manifest.mediaType(), "something");
    }

    @Test
    void shouldFailWhenMediaTypeIsAbsent() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            "{\"abc\":\"123\"}".getBytes()
        );
        Assertions.assertThrows(
            InvalidManifestException.class,
            manifest::mediaTypes
        );
    }

    @Test
    void shouldConvertToSameType() throws Exception {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            "{\"mediaType\":\"type2\"}".getBytes()
        );
        MatcherAssert.assertThat(
            manifest.convert(hashSet("type1", "type2")),
            new IsEqual<>(manifest)
        );
    }

    @Test
    void shouldConvertToWildcardType() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"), "{\"mediaType\":\"my-type\"}".getBytes()
        );
        MatcherAssert.assertThat(
            manifest.convert(hashSet("*/*")),
            new IsEqual<>(manifest)
        );
    }

    @Test
    void shouldConvertForMultiType() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("qwe"),
            "{\"mediaType\":\"application/vnd.oci.image.manifest.v1+json,application/vnd.docker.distribution.manifest.v2+json,application/vnd.docker.distribution.manifest.v1+json,application/vnd.docker.distribution.manifest.list.v2+json\"}".getBytes()
        );
        MatcherAssert.assertThat(
            manifest.convert(
                hashSet("application/vnd.docker.distribution.manifest.v2+json")
            ),
            new IsEqual<>(manifest)
        );
    }

    @Test
    void shouldReadConfig() {
        final String digest = "sha256:def";
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            Json.createObjectBuilder().add(
                "config",
                Json.createObjectBuilder().add("digest", digest)
            ).build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            manifest.config().string(),
            new IsEqual<>(digest)
        );
    }

    @Test
    void shouldReadLayerDigests() {
        final String[] digests = {"sha256:123", "sha256:abc"};
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("12345"),
            Json.createObjectBuilder().add(
                "layers",
                Json.createArrayBuilder(
                    Stream.of(digests)
                        .map(dig -> Collections.singletonMap("digest", dig))
                        .collect(Collectors.toList())
                )
            ).build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            manifest.layers().stream()
                .map(Layer::digest)
                .map(Digest::string)
                .collect(Collectors.toList()),
            Matchers.containsInAnyOrder(digests)
        );
    }

    @Test
    void shouldReadLayerUrls() throws Exception {
        final String url = "https://artipie.com/";
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            Json.createObjectBuilder().add(
                "layers",
                Json.createArrayBuilder().add(
                    Json.createObjectBuilder()
                        .add("digest", "sha256:12345")
                        .add(
                            "urls",
                            Json.createArrayBuilder().add(url)
                        )
                )
            ).build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            manifest.layers().stream()
                .flatMap(layer -> layer.urls().stream())
                .collect(Collectors.toList()),
            new IsIterableContaining<>(new IsEqual<>(URI.create(url).toURL()))
        );
    }

    @Test
    void shouldFailWhenLayersAreAbsent() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            "{\"any\":\"value\"}".getBytes()
        );
        Assertions.assertThrows(
            InvalidManifestException.class,
            manifest::layers
        );
    }

    @Test
    void shouldReadDigest() {
        final String digest = "sha256:123";
        final JsonManifest manifest = new JsonManifest(
            new Digest.FromString(digest),
            "{ \"schemaVersion\": 2 }".getBytes()
        );
        Assertions.assertEquals(manifest.digest().string(), digest);
    }

    @Test
    void shouldReadContent() {
        final byte[] data = "{ \"schemaVersion\": 2 }".getBytes();
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            data
        );
        MatcherAssert.assertThat(
            new PublisherAs(manifest.content()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
    }

    /**
     * Create new set from items.
     * @param items Items
     * @return Unmodifiable hash set
     */
    private static Set<? extends String> hashSet(final String... items) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(items)));
    }
}
