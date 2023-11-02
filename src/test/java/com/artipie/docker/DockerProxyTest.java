/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.adapters.docker.DockerProxy;
import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.policy.Policy;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.cache.StoragesCache;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.test.TestStoragesCache;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.hamcrest.CustomMatcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link DockerProxy}.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerProxyTest {

    /**
     * Storages caches.
     */
    private StoragesCache cache;

    @BeforeEach
    void setUp() {
        this.cache = new TestStoragesCache();
    }

    @ParameterizedTest
    @MethodSource("goodConfigs")
    void shouldBuildFromConfig(final String yaml) throws Exception {
        final Slice slice = dockerProxy(this.cache, yaml);
        MatcherAssert.assertThat(
            slice.response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ),
            new RsHasStatus(
                new IsNot<>(
                    new CustomMatcher<>("is server error") {
                        @Override
                        public boolean matches(final Object item) {
                            return ((RsStatus) item).serverError();
                        }
                    }
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("badConfigs")
    void shouldFailBuildFromBadConfig(final String yaml) throws Exception {
        final Slice slice = dockerProxy(this.cache, yaml);
        Assertions.assertThrows(
            RuntimeException.class,
            () -> slice.response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            ).toCompletableFuture().join()
        );
    }

    private static DockerProxy dockerProxy(
        final StoragesCache cache,
        final String yaml
    ) throws IOException {
        return new DockerProxy(
            new JettyClientSlices(),
            false,
            new RepoConfig(
                new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
                Key.ROOT,
                Yaml.createYamlInput(yaml).readYamlMapping(),
                cache
            ),
            Policy.FREE,
            (username, password) -> Optional.empty(),
            Optional.empty()
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> goodConfigs() {
        return Stream.of(
            "repo:\n  remotes:\n    - url: registry-1.docker.io",
            String.join(
                "\n",
                "repo:",
                "  type: docker-proxy",
                "  remotes:",
                "    - url: registry-1.docker.io",
                "      username: admin",
                "      password: qwerty",
                "      cache:",
                "        storage:",
                "          type: fs",
                "          path: /var/artipie/data/cache",
                "    - url: another-registry.org:54321",
                "    - url: mcr.microsoft.com",
                "      cache:",
                "        storage: ",
                "          type: fs",
                "          path: /var/artipie/data/local/cache",
                "  storage:",
                "    type: fs",
                "    path: /var/artipie/data/local"
            )
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> badConfigs() {
        return Stream.of(
            "",
            "repo:",
            "repo:\n  remotes:\n    - attr: value",
            "repo:\n  remotes:\n    - url: registry-1.docker.io\n      username: admin",
            "repo:\n  remotes:\n    - url: registry-1.docker.io\n      password: qwerty"
        );
    }
}
