/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.http.auth.Authentication;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link DockerPermissions}.
 *
 * @since 0.15
 */
class DockerPermissionsTest {

    @ParameterizedTest
    @CsvSource({
        "repository:my-alpine:pull,read",
        "repository:test/image:push,write",
        "registry:catalog:*,read"
    })
    void shouldTranslatePermissions(final String original, final String translated) {
        final AtomicReference<String> captured = new AtomicReference<>();
        new DockerPermissions(
            (user, action) -> {
                captured.set(action);
                return action.equals(translated);
            }
        ).allowed(new Authentication.User("alice"), original);
        MatcherAssert.assertThat(
            captured.get(),
            new IsEqual<>(translated)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldForwardResult(final boolean result) {
        MatcherAssert.assertThat(
            new DockerPermissions((user, action) -> result).allowed(
                new Authentication.User("bob"), "repository:my-test:pull"
            ),
            new IsEqual<>(result)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "repository:my-alpine:read",
        "whatever",
        "foo:bar:"
    })
    void shouldFailForUnexpectedActions(final String action) {
        final DockerPermissions permissions = new DockerPermissions((user, act) -> true);
        Assertions.assertThrows(
            RuntimeException.class,
            () -> permissions.allowed(new Authentication.User("chuck"), action)
        );
    }
}
