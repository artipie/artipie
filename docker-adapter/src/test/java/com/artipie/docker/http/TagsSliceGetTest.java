/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.asto.Uploads;
import com.artipie.docker.fake.FullTagsManifests;
import com.artipie.docker.misc.Pagination;
import com.artipie.http.Headers;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link DockerSlice}.
 * Tags list GET endpoint.
 */
class TagsSliceGetTest {

    @Test
    void shouldReturnTags() {
        final byte[] tags = "{...}".getBytes();
        final FakeDocker docker = new FakeDocker(
            new FullTagsManifests(() -> new Content.From(tags))
        );
        MatcherAssert.assertThat(
            "Responds with tags",
            new DockerSlice(docker),
            new SliceHasResponse(
                new ResponseMatcher(
                    RsStatus.OK,
                    Headers.from(
                        new ContentLength(tags.length),
                        ContentType.json()
                    ),
                    tags
                ),
                new RequestLine(RqMethod.GET, "/v2/my-alpine/tags/list")
            )
        );
        MatcherAssert.assertThat(
            "Gets tags for expected repository name",
            docker.capture.get(),
            Matchers.is("my-alpine")
        );
    }

    @Test
    void shouldSupportPagination() {
        final String from = "1.0";
        final int limit = 123;
        final FullTagsManifests manifests = new FullTagsManifests(() -> Content.EMPTY);
        final Docker docker = new FakeDocker(manifests);
        new DockerSlice(docker).response(
            new RequestLine(
                RqMethod.GET,
                String.format("/v2/my-alpine/tags/list?n=%d&last=%s", limit, from)
            ),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            "Parses from",
            manifests.capturedFrom(),
            Matchers.is(Optional.of(from))
        );
        MatcherAssert.assertThat(
            "Parses limit",
            manifests.capturedLimit(),
            Matchers.is(limit)
        );
    }

    /**
     * Docker implementation that returns repository with specified manifests
     * and captures repository name.
     *
     * @since 0.8
     */
    private static class FakeDocker implements Docker {

        /**
         * Repository manifests.
         */
        private final Manifests manifests;

        /**
         * Captured repository name.
         */
        private final AtomicReference<String> capture;

        FakeDocker(final Manifests manifests) {
            this.manifests = manifests;
            this.capture = new AtomicReference<>();
        }

        @Override
        public Repo repo(String name) {
            this.capture.set(name);
            return new Repo() {
                @Override
                public Layers layers() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Manifests manifests() {
                    return FakeDocker.this.manifests;
                }

                @Override
                public Uploads uploads() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public CompletableFuture<Catalog> catalog(Pagination pagination) {
            throw new UnsupportedOperationException();
        }
    }
}
