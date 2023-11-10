/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Content;
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
import com.artipie.scheduling.MetadataEventQueues;
import com.artipie.security.policy.Policy;
import com.artipie.settings.ArtipieSecurity;
import com.artipie.settings.MetricsContext;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.CachedUsers;
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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

    @Test
    void notAllowsAnyUser() {
        MatcherAssert.assertThat(
            new DockerRoutingSlice(
                new SettingsWithAuth((usr, pswd) -> Optional.of(Authentication.ANY_USER)),
                (line, headers, body) -> {
                    throw new UnsupportedOperationException();
                }
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                new RequestLine(RqMethod.GET, "/v2/"),
                new Headers.From(new Authorization.Basic("any", "ignored")),
                Content.EMPTY
            )
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
        private final Authentication auth;

        SettingsWithAuth(final Authentication auth) {
            this.auth = auth;
        }

        @Override
        public Storage configStorage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ArtipieSecurity authz() {
            return new ArtipieSecurity() {

                @Override
                public CachedUsers authentication() {
                    return new CachedUsers(SettingsWithAuth.this.auth);
                }

                @Override
                public Policy<?> policy() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Optional<Storage> policyStorage() {
                    return Optional.empty();
                }
            };
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

        @Override
        public Optional<MetadataEventQueues> artifactMetadata() {
            return Optional.empty();
        }

        @Override
        public Optional<YamlSequence> crontab() {
            return Optional.empty();
        }
    }
}
