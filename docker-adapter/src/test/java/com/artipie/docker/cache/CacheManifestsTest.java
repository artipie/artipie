/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.asto.Content;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.Layers;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.Uploads;
import com.artipie.docker.fake.FakeManifests;
import com.artipie.docker.fake.FullTagsManifests;
import com.artipie.docker.manifest.Manifest;
import com.artipie.scheduling.ArtifactEvent;
import com.google.common.base.Stopwatch;
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

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link CacheManifests}.
 */
final class CacheManifestsTest {
    @ParameterizedTest
    @CsvSource({
        "empty,empty,",
        "empty,full,cache",
        "full,empty,origin",
        "faulty,full,cache",
        "full,faulty,origin",
        "faulty,empty,",
        "empty,faulty,",
        "full,full,origin"
    })
    void shouldReturnExpectedValue(
        final String origin,
        final String cache,
        final String expected
    ) {
        final CacheManifests manifests = new CacheManifests(
            new RepoName.Simple("test"),
            new SimpleRepo(new FakeManifests(origin, "origin")),
            new SimpleRepo(new FakeManifests(cache, "cache")),
            Optional.empty(), "*"
        );
        MatcherAssert.assertThat(
            manifests.get(ManifestReference.from("ref"))
                .toCompletableFuture().join()
                .map(Manifest::digest)
                .map(Digest::hex),
            new IsEqual<>(Optional.ofNullable(expected))
        );
    }

    @Test
    void shouldCacheManifest() throws Exception {
        final ManifestReference ref = ManifestReference.from("1");
        final Queue<ArtifactEvent> events = new ConcurrentLinkedQueue<>();
        final Repo cache = new AstoDocker(new LoggingStorage(new InMemoryStorage()))
            .repo(new RepoName.Simple("my-cache"));
        new CacheManifests(
            new RepoName.Simple("cache-alpine"),
            new AstoDocker(new ExampleStorage()).repo(new RepoName.Simple("my-alpine")),
            cache, Optional.of(events), "my-docker-proxy"
        ).get(ref).toCompletableFuture().join();
        final Stopwatch stopwatch = Stopwatch.createStarted();
        while (cache.manifests().get(ref).toCompletableFuture().join().isEmpty()) {
            final int timeout = 10;
            if (stopwatch.elapsed(TimeUnit.SECONDS) > timeout) {
                break;
            }
            final int pause = 100;
            Thread.sleep(pause);
        }
        MatcherAssert.assertThat(
            String.format(
                "Manifest is expected to be present, but it was not found after %s seconds",
                stopwatch.elapsed(TimeUnit.SECONDS)
            ),
            cache.manifests().get(ref).toCompletableFuture().join().isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Artifact metadata were added to queue", events.size() == 1
        );
        final ArtifactEvent event = events.poll();
        MatcherAssert.assertThat(
            event.artifactName(), new IsEqual<>("cache-alpine")
        );
        MatcherAssert.assertThat(
            event.artifactVersion(), new IsEqual<>("1")
        );
    }

    @Test
    void loadsTagsFromOriginAndCache() {
        final int limit = 3;
        final String name = "tags-test";
        MatcherAssert.assertThat(
            new CacheManifests(
                new RepoName.Simple(name),
                new SimpleRepo(
                    new FullTagsManifests(
                        () -> new Content.From("{\"tags\":[\"one\",\"three\",\"four\"]}".getBytes())
                    )
                ),
                new SimpleRepo(
                    new FullTagsManifests(
                        () -> new Content.From("{\"tags\":[\"one\",\"two\"]}".getBytes())
                    )
                ), Optional.empty(), "*"
            ).tags(Optional.of(new Tag.Valid("four")), limit).thenCompose(
                tags -> tags.json().asStringFuture()
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

    /**
     * Simple repo implementation.
     *
     * @since 0.3
     */
    private static final class SimpleRepo implements Repo {
        /**
         * Manifests.
         */
        private final Manifests mnfs;

        /**
         * Ctor.
         *
         * @param mnfs Manifests.
         */
        private SimpleRepo(final Manifests mnfs) {
            this.mnfs = mnfs;
        }

        @Override
        public Layers layers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Manifests manifests() {
            return this.mnfs;
        }

        @Override
        public Uploads uploads() {
            throw new UnsupportedOperationException();
        }
    }
}
