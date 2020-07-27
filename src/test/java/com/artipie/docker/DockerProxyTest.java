/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.docker;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.RepoConfig;
import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Permissions;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.eclipse.jetty.client.HttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link DockerProxy}.
 *
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DockerProxyTest {

    @ParameterizedTest
    @MethodSource("goodConfigs")
    void shouldBuildFromConfig(final String yaml) throws Exception {
        final Slice slice = dockerProxy(yaml);
        MatcherAssert.assertThat(
            slice.response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @ParameterizedTest
    @MethodSource("badConfigs")
    void shouldFailBuildFromBadConfig(final String yaml) throws Exception {
        final Slice slice = dockerProxy(yaml);
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

    private static DockerProxy dockerProxy(final String yaml) throws IOException {
        return new DockerProxy(
            new HttpClient(),
            new RepoConfig(
                alias -> {
                    throw new UnsupportedOperationException();
                },
                Key.ROOT,
                Yaml.createYamlInput(yaml).readYamlMapping()
            ),
            Permissions.FREE,
            (username, password) -> Optional.empty()
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
                "        storage: my-storage",
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
            "repo:\n  remotes:\n    - url: registry-1.docker.io\n      password: qwerty",
            "repo:\n  remotes:\n    - url: registry-1.docker.io\n      cache:"
        );
    }
}
