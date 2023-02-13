/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.AssertSlice;
import com.artipie.http.hm.RqLineHasUri;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.settings.Layout;
import com.artipie.settings.MetricsContext;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.test.TestArtipieCaches;
import com.artipie.test.TestSettings;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DockerRoutingSlice}.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class DockerRoutingSliceTest {

    @Test
    void removesDockerPrefix() throws Exception {
        verify(
            new DockerRoutingSlice(
                new TestSettings(),
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath("/foo/bar")))
            ),
            "/v2/foo/bar"
        );
    }

    @Test
    void ignoresNonDockerRequests() throws Exception {
        final String path = "/repo/name";
        verify(
            new DockerRoutingSlice(
                new TestSettings(),
                new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath(path)))
            ),
            path
        );
    }

    @Test
    void emptyDockerRequest() {
        final String username = "alice";
        final String password = "letmein";
        MatcherAssert.assertThat(
            new DockerRoutingSlice(
                new SettingsWithAuth(new Authentication.Single(username, password)),
                (line, headers, body) -> {
                    throw new UnsupportedOperationException();
                }
            ),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasHeaders(
                            new Headers.From("Docker-Distribution-API-Version", "registry/2.0")
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/v2/"),
                new Headers.From(new Authorization.Basic(username, password)),
                Content.EMPTY
            )
        );
    }

    @Test
    void revertsDockerRequest() throws Exception {
        final String path = "/v2/one/two";
        verify(
            new DockerRoutingSlice(
                new TestSettings(),
                new DockerRoutingSlice.Reverted(
                    new AssertSlice(new RqLineHasUri(new RqLineHasUri.HasPath(path)))
                )
            ),
            path
        );
    }

    private static void verify(final Slice slice, final String path) throws Exception {
        slice.response(
            new RequestLine(RqMethod.GET, path).toString(),
            Collections.emptyList(), Flowable.empty()
        ).send(
            (status, headers, body) -> CompletableFuture.completedFuture(null)
        ).toCompletableFuture().get();
    }

    /**
     * Fake settings with auth.
     *
     * @since 0.10
     */
    private static class SettingsWithAuth implements Settings {

        /**
         * Authentication.
         */
        @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
        private final Authentication auth;

        SettingsWithAuth(final Authentication auth) {
            this.auth = auth;
        }

        @Override
        public Storage configStorage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Authentication auth() {
            return this.auth;
        }

        @Override
        public Layout layout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public YamlMapping meta() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Storage repoConfigsStorage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Key> credentialsKey() {
            return Optional.empty();
        }

        @Override
        public Optional<KeyStore> keyStore() {
            return Optional.empty();
        }

        @Override
        public MetricsContext metrics() {
            return null;
        }

        @Override
        public ArtipieCaches caches() {
            return new TestArtipieCaches();
        }
    }
}
