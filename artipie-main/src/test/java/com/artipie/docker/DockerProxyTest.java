/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.adapters.docker.DockerProxy;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.cache.StoragesCache;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.security.policy.Policy;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.test.TestStoragesCache;
import org.hamcrest.CustomMatcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tests for {@link DockerProxy}.
 */
class DockerProxyTest {

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
                new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY
            ).join(),
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
    void shouldFailBuildFromBadConfig(final String yaml) {
        Assertions.assertThrows(
            RuntimeException.class,
            () -> dockerProxy(this.cache, yaml).response(
                new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY
            ).join()
        );
    }

    private static DockerProxy dockerProxy(
        StoragesCache cache, String yaml
    ) throws IOException {
        return new DockerProxy(
            new JettyClientSlices(),
            RepoConfig.from(
                Yaml.createYamlInput(yaml).readYamlMapping(),
                new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
                Key.ROOT, cache, false
            ),
            Policy.FREE,
            (username, password) -> Optional.empty(),
            Optional.empty()
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> goodConfigs() {
        return Stream.of(
            "repo:\n  type: docker-proxy\n  remotes:\n    - url: registry-1.docker.io",
            String.join(
                "\n",
                "repo:",
                "  type: docker-proxy",
                "  remotes:",
                "    - url: registry-1.docker.io",
                "      username: admin",
                "      password: qwerty",
                "      priority: 1500",
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
